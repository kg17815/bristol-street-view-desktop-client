package client;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.junit.Test;
import org.junit.Assert;


import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DatabaseConnectionTest {

    @Test
    public void singleInsertTest() {
        try (DatabaseConnection db = new DatabaseConnection()) {
            db.deleteAll();
            int result = db.insertPhotoRow(
                    "1234567",
                    1000,
                    1000,
                    LocalDateTime.of(2017, Month.AUGUST, 1, 13, 45),
                    LocalDateTime.of(2018, Month.JULY, 23, 12, 31),
                    123.34455,
                    4555.5600054,
                    "12345567",
                    12,
                    "test-bucket",
                    "test-key"
            );

            Assert.assertEquals("DB result doesn't match", 1, result);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void multipleInsertTest() {
        singleInsertTest();
        try (DatabaseConnection db = new DatabaseConnection()) {
            db.deleteAll();

            int result = db.insertPhotoRow(
                    "1234567",
                    1000,
                    1000,
                    LocalDateTime.of(2017, Month.AUGUST, 1, 13, 45),
                    LocalDateTime.of(2018, Month.JULY, 23, 12, 31),
                    123.34455,
                    4555.5600054,
                    "12345567",
                    12,
                    "test-bucket",
                    "test-key"
            );

            Assert.assertEquals("DB result doesn't match", 1, result);

            result = db.insertPhotoRow(
                    "1234569",
                    1003,
                    999,
                    LocalDateTime.of(2017, Month.AUGUST, 2, 13, 45),
                    LocalDateTime.of(2018, Month.JULY, 23, 12, 33),
                    12.5322,
                    455.5600054,
                    "12345567",
                    11,
                    "test-bucket",
                    "test-key-2"
            );

            Assert.assertEquals("DB result doesn't match", 1, result);

            result = db.insertPhotoRow(
                    "1234570",
                    1003,
                    999,
                    LocalDateTime.of(2017, Month.AUGUST, 2, 13, 50),
                    LocalDateTime.of(2018, Month.JULY, 23, 12, 34),
                    12.5323,
                    455.5600055,
                    "12345567",
                    11,
                    "test-bucket",
                    "test-key-3"
            );

            Assert.assertEquals("DB result doesn't match", 1, result);

            result = db.insertPhotoRow(
                    "1234571",
                    1003,
                    999,
                    LocalDateTime.of(2017, Month.AUGUST, 2, 13, 51),
                    LocalDateTime.of(2018, Month.JULY, 23, 12, 35),
                    12.5345,
                    455.5600060,
                    "12345567",
                    11,
                    "test-bucket",
                    "test-key-4"
            );

            Assert.assertEquals("DB result doesn't match", 1, result);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void deleteTest() {
        multipleInsertTest();

        try (DatabaseConnection db = new DatabaseConnection()) {
            db.deleteAll();
        } catch (Exception e) {
            e.printStackTrace();
        }

        singleInsertTest();
    }

    @Test
    public void getPhotoTest() throws SQLException {
        FilePath path = null;

        try (DatabaseConnection db = new DatabaseConnection()) {
            db.deleteAll();
            multipleInsertTest();
            path = db.getPath("1234570");
        }
        Assert.assertEquals("Wrong bucket", "test-bucket", path.getBucket());
        Assert.assertEquals("Wrong key", "test-key-3", path.getKey());
    }

    @Test (expected = SQLException.class)
    public void getPhotoNoMatchTest() throws SQLException {
        FilePath path = null;

        try (DatabaseConnection db = new DatabaseConnection()) {
            db.deleteAll();
            multipleInsertTest();
            path = db.getPath("1234");
        }
        Assert.assertEquals("Wrong bucket", "test-bucket", path.getBucket());
        Assert.assertEquals("Wrong key", "test-key", path.getKey());
    }

    @Test
    public void getPhotoGPS() throws SQLException {

        List<String> ids = new ArrayList<>();

        try (DatabaseConnection db = new DatabaseConnection()) {
            db.deleteAll();
            multipleInsertTest();
            ids = db.getPhotosTakenAt(123.34455, 4555.5600054);
        }

        Assert.assertEquals("List must only have one element",1, ids.size());

        String id = ids.get(0);

        Assert.assertEquals("Wrong id", "1234567", id);

    }

    @Test (expected = SQLException.class)
    public void getPhotoGPSNoMatchTest() throws SQLException {
        List<String> ids = new ArrayList<>();

        try (DatabaseConnection db = new DatabaseConnection()) {
            multipleInsertTest();
            ids = db.getPhotosTakenAt(123.3445, 4555.5600054);
        }
    }

    @Test
    public void getPhotoDateTakenTest() throws SQLException {
        List<String> ids = new ArrayList<>();

        try (DatabaseConnection db = new DatabaseConnection()) {
            multipleInsertTest();
            ids = db.getPhotosTakenOn(LocalDateTime.of(2017, Month.AUGUST, 2, 13, 45));
        }

        Assert.assertEquals("List must only have one element", 1, ids.size());

        String id = ids.get(0);

        Assert.assertEquals("Wrong id", "1234569", id);

    }

    @Test (expected = SQLException.class)
    public void getPhotoDateTakenNoMatchTest() throws SQLException {
        try (DatabaseConnection db = new DatabaseConnection()) {
            multipleInsertTest();
            db.getPhotosTakenOn(LocalDateTime.of(2017, Month.AUGUST, 3, 13, 45));
        }
    }

    @Test
    public void getPhotoDateUploadedTest() throws SQLException {
        List<String> ids = new ArrayList<>();

        try (DatabaseConnection db = new DatabaseConnection()) {
            multipleInsertTest();
            ids = db.getPhotosUploadedOn(LocalDateTime.of(2018, Month.JULY, 23, 12, 34));
        }

        Assert.assertEquals("List must only have one element", 1, ids.size());

        String id = ids.get(0);

        Assert.assertEquals("Wrong id", "1234570", id);
    }

    @Test (expected = SQLException.class)
    public void getPhotoDateUploadedNoMatchTest() throws SQLException {
        try (DatabaseConnection db = new DatabaseConnection()) {
            multipleInsertTest();
            db.getPhotosUploadedOn(LocalDateTime.of(2018, Month.JULY, 23, 12, 38));
        }

    }

    @Test
    public void getPhotoBetweenDateTakenTest() throws SQLException {
        List<String> ids = new ArrayList<>();

        try (DatabaseConnection db = new DatabaseConnection()) {
            multipleInsertTest();
            ids = db.getPhotosTakenBetween(
                    LocalDateTime.of(2017, Month.AUGUST, 2, 13, 45),
                    LocalDateTime.of(2017, Month.AUGUST, 2, 13, 55));

        }

        Assert.assertEquals("Incorrect size", 3, ids.size());

        Set<String> set = new HashSet<>(ids);

        Assert.assertTrue("Missing id", set.contains("1234569"));
        Assert.assertTrue("Missing id", set.contains("1234570"));
        Assert.assertTrue("Missing id", set.contains("1234571"));

    }

    @Test
    public void getPhotoBetweenDateUploadedTest() throws SQLException {
        List<String> ids = new ArrayList<>();

        try (DatabaseConnection db = new DatabaseConnection()) {
            multipleInsertTest();
            ids = db.getPhotosUploadedBetween(
                    LocalDateTime.of(2018, Month.JULY, 23, 12, 31),
                    LocalDateTime.of(2018, Month.JULY, 23, 12, 33));

        }

        Assert.assertEquals("Incorrect size", 2, ids.size());

        Set<String> set = new HashSet<>(ids);

        Assert.assertTrue("Missing id", set.contains("1234567"));
        Assert.assertTrue("Missing id", set.contains("1234569"));
    }
}