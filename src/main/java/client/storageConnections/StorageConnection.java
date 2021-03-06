package client.storageConnections;

import client.handler.FileHolder;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometry;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;

/**
 * An interface for interacting with a photo file storage.
 *
 * Each implementation has a corresponding {@link StorageType} enum.
 * File can only accepted in the form of a {@link FileHolder}.
 * One instance of a Storage Connection should only be responsible for uploading one file.
 *
 * @author Chris Gora
 * @version 1.0, 01.09.2018
 */
public abstract class StorageConnection {

    static final String RTREE_FILE = "rtree.tree";
    final FileHolder fileHolder;
    private final StorageType type;

    StorageConnection(FileHolder fileHolder, StorageType type) {
        this.fileHolder = fileHolder;
        this.type = type;
    }

    /**
     * Returns the {@link StorageType} enum associated with this Connection.
     */
    public StorageType getType() {
        return type;
    }

    /**
     * Returns the {@link FileHolder} associated with this connection.
     * Only one FileHolder can be associated with one connection.
     */
    public FileHolder getFileHolder() {
        return fileHolder;
    }

    /**
     * Uploads / copies the file into the specified bucket.
     * Calls the following callback methods:
     *      <li> {@link FileHolder#onBytesUploaded(long)}
     *      <li> {@link FileHolder#onUploadFailure(String)}
     *
     * Should be submitted as a Runnable and not executed on the main thread.
     */
    public abstract void copyFile();

    /**
     * Copies the file into the output sub directory of the specified bucket.
     *
     * Should be submitted as a Runnable and not executed on the main thread.
     */
    public abstract void copyFileToOutput();

    /**
     * Removes a file from the bucket.
     * Calls the following callback methods:
     *      <li> {@link FileHolder#onRemoveSuccess()}
     *      <li> {@link FileHolder#onRemoveFailure(String)}
     *
     * Should be submitted as a Runnable and not executed on the main thread.
     */
    public abstract void removeFile();

    /**
     * Removes all files from a bucket (and that bucket only).
     * Calls the following callback methods:
     *      <li> {@link FileHolder#onRemoveSuccess()}
     *      <li> {@link FileHolder#onRemoveFailure(String)}
     *
     * Should be submitted as a Runnable and not executed on the main thread.
     */
    public abstract void removeAll() throws IOException;

    /**
     * Returns a file for the given Image ID. The file may be null.
     * This new file should be based on the bucket and key supplied with the file holder
     */
    @Nullable
    public abstract File getFile(String key) throws IOException;

    // TODO: 30/08/18 Javadoc
    public abstract Optional<RTree<String, Geometry>> getRTree();

    // TODO: 30/08/18 Javadoc
    public abstract void saveRTree(RTree<String, Geometry> tree) throws IOException;

    public abstract void clearOutput() throws IOException;
}
