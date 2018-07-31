package client;

import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.MetadataException;
import io.jenetics.jpx.GPX;
import io.jenetics.jpx.WayPoint;
import org.apache.commons.imaging.ImageReadException;

import javax.swing.text.Segment;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Uploader {

    private static final String BUCKET = "bristol-streetview-photos";

    private StorageType type;
    private ExecutorService executor;
    private StorageConnection storageConnection;
    private List<FileHolder> doneUploads;

    Uploader(StorageType type) {
        this.executor = Executors.newFixedThreadPool(2);
        this.executor = new DebuggingExecutor(2, 2, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1000));
        this.doneUploads = new ArrayList<>();
        this.type = type;
    }

    private StorageConnection getStorageConnection(FileHolder fileHolder) {
        switch (type) {
            case AMAZON:
                return new S3Connection(fileHolder);

            case LOCAL:
                return new LocalStorageConnection(fileHolder);

                default:
                    return new LocalStorageConnection(fileHolder);
        }
    }

    public void stop() {
        executor.shutdown();
    }

    public FileHolder newUploadHolder(File file) {
        FileHolder fileHolder = new FileHolder();
        fileHolder.setFile(file);
        return fileHolder;
    }

    public void upload(FileHolder upload) {

        ImageMetadata metadata = null;
        try {
            metadata = new ImageMetadata(upload.getFile());
        } catch (IOException | MetadataException | ImageProcessingException | ImageReadException e) {
            e.printStackTrace();
            upload.onUploadFailure(e.toString());
            return;
        }

//        FileHolder upload = new FileHolder();
//        upload.setFile(file);
        upload.setMetadata(metadata);

        String id = null;
        id = metadata.getId();


        if (id == null) {
//            System.out.println("Image ID was null");
            if (upload.getFile().getName().contains("_E")) {
                upload.onUploadFailure("Image ID was null");
                return;
            }

            else {
                id = UUID.randomUUID().toString().replace("-", "");
                metadata.setId(id);
            }
        }

        String key = id + "-" + upload.getFile().getName();
        System.out.println(key);

        upload.setKey(key);
        upload.setBucket(BUCKET);

//        Runnable storageConnection = new S3Connection(upload);
        StorageConnection storageConnection = getStorageConnection(upload);

//        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(storageConnection::copyFile);

        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>> SUBMITTED");
        upload.setUploadCompletionListener(this::updateDatabase);
        upload.setUploadCompletionListener(doneUploads::add);
//        return upload;
    }

    private void updateDatabase(FileHolder upload) {

        LocalDateTime localDateTime = LocalDateTime.now();

        executor.submit(() -> {
            try (DatabaseConnection db = new DatabaseConnection()) {
                System.out.println("Uploader established a DB connection");
//            System.out.println(upload.getBucket());
//            System.out.println(upload.getKey());
                ImageMetadata metadata = upload.getMetadata();
                int result = db.insertPhotoRow(
                        metadata.getId(),
                        metadata.getHeight(),
                        metadata.getWidth(),
//                        new Timestamp((new Date()).getTime()),
//                        new Timestamp(new Date().getTime()),
                        metadata.getPhotoDateTime(),
                        localDateTime,
                        metadata.getLatitude(),
                        metadata.getLongitude(),
                        metadata.getSerialNumber(),
                        1,
                        upload.getBucket(),
                        upload.getKey()
                );

                if (result == 1) {
                    upload.onDbSuccess();
                } else {
                    upload.onDbFailure("Database error - database returned: " + result);

                    upload.setRemoveCompletionListener((f) -> System.out.println("REMOVE Done: " + f.getKey()));
                    upload.setRemoveFailureListener(System.out::println);

                    StorageConnection storageConnection = getStorageConnection(upload);
                    executor.submit(storageConnection::removeFile);
                    // TODO: 23/07/18 Remove the photo from file storage if it was rejected by the database

                }

            } catch (Exception e) {
                e.printStackTrace();
                upload.onDbFailure(e.toString());

                upload.setRemoveCompletionListener((f) -> System.out.println("REMOVE Done: " + f.getKey()));
                upload.setRemoveFailureListener(System.out::println);

                StorageConnection storageConnection = getStorageConnection(upload);
                executor.submit(storageConnection::removeFile);

                // FIXME: 31/07/18 Duplicate code for removing dead files
            }
        });
    }

    public void saveJustUploadedAsNewRoute(int routeId) {
//        WayPoint wayPoint = WayPoint.builder().lon(12.00).lat(13.00).build();

        System.out.println("HERE!!!");

        List<WayPoint> wayPoints = new ArrayList<>();

        System.out.println("done size: " + doneUploads.size());

        for (FileHolder fileHolder : doneUploads) {
            double longitude = fileHolder.getMetadata().getLongitude();
            double latitude = fileHolder.getMetadata().getLatitude();
            Instant instant = fileHolder.getMetadata().getPhotoDateTime().toInstant(ZoneOffset.ofTotalSeconds(0));
            WayPoint wayPoint = WayPoint.builder().lon(longitude).lat(latitude).time(instant).build();
            wayPoints.add(wayPoint);
        }

        System.out.println("Waypoints size: " + wayPoints.size());

        GPX gpx = GPX.builder()
                .addTrack(t -> t
                .addSegment(s -> wayPoints.forEach(s::addPoint)))
                .build();

        System.out.println("builder done");

        // TODO: 30/07/18 Write the GPX file in the appropriate file store - Amazon or Local storage - but needs to be decided by THE STORAGE CLASS

        try {
            System.out.println("inside try");
            File tempFile = File.createTempFile("JPX" + routeId, ".gpx");
            GPX.write(gpx, tempFile.getPath());
            FileHolder fileHolder = new FileHolder();
            fileHolder.setFile(tempFile);
            fileHolder.setBucket(BUCKET);
            fileHolder.setKey("GPX_" + routeId + "_" + UUID.randomUUID().toString().replace("-", "") + ".gpx");

            fileHolder.setUploadCompletionListener(f -> {
                System.out.println("GPX UPLOAD DONE!!!");
                doneUploads.clear();
                tempFile.delete();
            });

            fileHolder.setProgressListener(System.out::println);
            fileHolder.setUploadFailureListener(System.out::println);

            StorageConnection storageConnection = getStorageConnection(fileHolder);
            executor.submit(storageConnection::copyFile);

            System.out.println("submited");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteAll() {
        try (DatabaseConnection db = new DatabaseConnection()) {
            db.deleteAll();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}