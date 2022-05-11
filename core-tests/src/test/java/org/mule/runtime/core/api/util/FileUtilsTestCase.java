/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util;

import static java.io.File.separator;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mule.runtime.core.api.util.FileUtils.createFile;
import static org.mule.runtime.core.api.util.FileUtils.deleteTree;
import static org.mule.runtime.core.api.util.FileUtils.extractResources;
import static org.mule.runtime.core.api.util.FileUtils.newFile;
import static org.mule.runtime.core.api.util.FileUtils.openDirectory;
import static org.mule.runtime.core.api.util.FileUtils.prepareWinFilename;
import static org.mule.runtime.core.api.util.FileUtils.renameFile;
import static org.mule.runtime.core.api.util.FileUtils.stringToFile;
import static org.mule.runtime.core.api.util.FileUtils.unzip;
import static org.mule.tck.ZipUtils.compress;

import org.mule.runtime.core.api.util.compression.InvalidZipFileException;
import org.mule.tck.ZipUtils.ZipResource;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class FileUtilsTestCase extends AbstractMuleTestCase {

  private static final String TEST_FILE = "testFile.txt";
  private static final String TEST_DIRECTORY = "target" + separator + "testDirectory";
  private static final String UNZIPPED_FILE_PATH =
      TEST_DIRECTORY + separator + "testFolder" + separator + "testFile.txt";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private File toDir;

  @Rule
  public ExpectedException thrownException = ExpectedException.none();

  @Before
  public void setupDir() {
    toDir = temporaryFolder.getRoot();
  }

  @Test
  public void testFileTools() throws Exception {
    File file = null;
    try {
      file = stringToFile(TEST_FILE, "this is a test file");
      assertNotNull(file);
      assertTrue(file.exists());

      file = stringToFile(TEST_FILE, " and this is appended content", true);

      String content = readFileToString(newFile(TEST_FILE), (String) null);

      assertNotNull(content);
      assertTrue(content.indexOf("this is a test file") > -1);
      assertTrue(content.indexOf(" and this is appended content") > -1);

      file = newFile(TEST_FILE);
      assertNotNull(file);
      assertTrue(file.exists());

      file = createFile(TEST_FILE);
      assertNotNull(file);
      assertTrue(file.exists());

      file = createFile(TEST_FILE + "2");
      assertNotNull(file);
      assertTrue(file.exists());
      assertTrue(file.canRead());
      file.delete();

      file = newFile(TEST_FILE);
      file.delete();

      File dir = openDirectory("src");
      assertNotNull(dir);
      assertTrue(dir.exists());
      assertTrue(dir.canRead());
      assertTrue(dir.isDirectory());

      dir = openDirectory("doesNotExist");
      assertNotNull(dir);
      assertTrue(dir.exists());
      assertTrue(dir.canRead());
      assertTrue(dir.isDirectory());
      dir.delete();

    } finally {
      if (file != null) {
        file.delete();
      }
    }
  }

  @Test
  public void testFileNameTools() throws Exception {
    String filename = "Blah<Blah>.txt";
    String result = prepareWinFilename(filename);
    assertEquals("Blah(Blah).txt", result);

    filename = "Bla]h<Blah:a;b|c?d=e_f*g>.txt";
    result = prepareWinFilename(filename);
    assertEquals("Bla-h(Blah-a-b-c-d=e_f-g).txt", result);

    filename = "B\"la-h<Blah:a;b|c?d=e_f*g>.txt";
    result = prepareWinFilename(filename);
    assertEquals("B-la-h(Blah-a-b-c-d=e_f-g).txt", result);
  }

  @Test
  public void testDirectoryTools() throws Exception {
    File dir = openDirectory("src");
    assertNotNull(dir);
    assertTrue(dir.exists());
    assertTrue(dir.canRead());
    assertTrue(dir.isDirectory());

    dir = openDirectory("doesNotExist");
    assertNotNull(dir);
    assertTrue(dir.exists());
    assertTrue(dir.canRead());
    assertTrue(dir.isDirectory());
    deleteTree(dir);
  }

  @Test
  public void testExtractResource() throws Exception {
    String testDir = TEST_DIRECTORY + separator + "Test-1";
    File outputDir = newFile(testDir);
    if (!outputDir.exists()) {
      assertTrue("Failed to create output dirs.", outputDir.mkdirs());
    }
    String res = "META-INF/MANIFEST.MF";
    extractResources(res, getClass(), outputDir, true);
    File result = newFile(testDir, res);
    assertNotNull(result);
    assertTrue(result.exists());
    assertTrue(result.canRead());
    assertTrue(result.isFile());
    assertTrue(result.length() > 0);
    deleteTree(outputDir);
  }

  @Test
  public void testExtractResources() throws Exception {
    String testDir = TEST_DIRECTORY + separator + "Test-2";
    File outputDir = newFile(testDir);
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }
    String res = "META-INF/";
    extractResources(res, getClass(), outputDir, true);
    File result = newFile(testDir, res);
    assertNotNull(result);
    assertTrue(result.exists());
    assertTrue(result.canRead());
    assertTrue(result.isDirectory());
    deleteTree(outputDir);
  }

  @Test
  public void testExtractFileResource() throws Exception {
    String testDir = TEST_DIRECTORY + separator + "Test-3";
    File outputDir = newFile(testDir);
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }
    String res = "org/mule/runtime/core/api/util/FileUtils.class";
    extractResources(res, FileUtils.class, outputDir, true);
    File result = newFile(testDir, res);
    assertNotNull(result);
    assertTrue(result.exists());
    assertTrue(result.canRead());
    assertTrue(result.isFile());
    assertTrue(result.length() > 0);
    deleteTree(outputDir);
  }

  @Test
  public void testExtractFileResources() throws Exception {
    String testDir = TEST_DIRECTORY + separator + "Test-4";
    File outputDir = newFile(testDir);
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }
    String res = "org/mule/runtime/core/api/util/";
    extractResources(res, FileUtils.class, outputDir, true);
    File result = newFile(testDir, res);
    assertNotNull(result);
    assertTrue(result.exists());
    assertTrue(result.canRead());
    assertTrue(result.isDirectory());
    deleteTree(outputDir);
  }

  @Test
  public void testExtractResourceWithoutKeepingDirStructure() throws Exception {
    String testDir = TEST_DIRECTORY + separator + "Test-5";
    File outputDir = newFile(testDir);
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }
    String fileName = "MANIFEST.MF";
    String res = "META-INF/" + fileName;
    extractResources(res, getClass(), outputDir, false);
    File result = newFile(testDir, fileName);
    assertNotNull(result);
    assertTrue(result.exists());
    assertTrue(result.canRead());
    assertTrue(result.isFile());
    assertTrue(result.length() > 0);
    deleteTree(outputDir);
  }

  @Test
  public void testExtractResourcesWithoutKeepingDirStructure() throws Exception {
    String testDir = TEST_DIRECTORY + separator + "Test-6";
    File outputDir = newFile(testDir);
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }
    String fileName = "util/FileUtilsTestCase.class";
    String res = "org/mule/runtime/core/api";
    extractResources(res, FileUtilsTestCase.class, outputDir, false);
    File result = newFile(testDir, fileName);
    assertNotNull(result);
    assertTrue(result.exists());
    assertTrue(result.canRead());
    assertTrue(result.isFile());
    assertTrue(result.length() > 0);
    deleteTree(outputDir);
  }

  @Test
  public void testExtractFileResourceWithoutKeepingDirStructure() throws Exception {
    String testDir = TEST_DIRECTORY + separator + "Test-7";
    File outputDir = newFile(testDir);
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }
    String fileName = "FileUtils.class";
    String res = "org/mule/runtime/core/api/util/" + fileName;
    extractResources(res, FileUtils.class, outputDir, false);
    File result = newFile(testDir, fileName);
    assertNotNull(result);
    assertTrue(result.exists());
    assertTrue(result.canRead());
    assertTrue(result.isFile());
    assertTrue(result.length() > 0);
    deleteTree(outputDir);
  }

  @Test
  public void testExtractFileResourcesWithoutKeepingDirStructure() throws Exception {
    String testDir = TEST_DIRECTORY + separator + "Test-8";
    File outputDir = newFile(testDir);
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }
    String fileName = "util/FileUtilsTestCase.class";
    String res = "org/mule/runtime/core/api/";
    extractResources(res, FileUtilsTestCase.class, outputDir, false);
    File result = newFile(testDir, fileName);
    assertNotNull(result);
    assertTrue(result.exists());
    assertTrue(result.canRead());
    assertTrue(result.isFile());
    assertTrue(result.length() > 0);
    deleteTree(outputDir);
  }

  @Test
  public void testDeleteTreeWithIgnoredDirectories() throws Exception {
    final String testDir = TEST_DIRECTORY + separator + "Test-deleting";
    File outputDir = newFile(testDir);
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }

    File toBeDeleted1 = newFile(outputDir, "toBeDeleted1/");
    toBeDeleted1.mkdirs();
    File toBeDeleted2 = newFile(outputDir, "toBeDeleted2/");
    toBeDeleted2.mkdirs();

    File keepMeIntact = newFile(outputDir, "keepMeIntact/");
    keepMeIntact.mkdirs();

    deleteTree(outputDir, new String[] {"keepMeIntact"});

    assertTrue("Shouldn't have been deleted.", keepMeIntact.exists());

    deleteTree(outputDir);
  }

  @Test
  public void testRenameFile() {
    try {
      File sourceFile = createTestFile("source");
      File destFile = createTestFile("dest");

      assertTrue(destFile.delete());
      assertTrue(renameFile(sourceFile, destFile));
      assertTrue(destFile.exists());
      assertTrue(destFile.delete());
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testRenameFileAcrossFolders() {
    try {
      File dir = createTestDir("test");
      File sourceFile = createTestFile("source");
      File destFile = new File(dir, "dest");

      assertTrue(renameFile(sourceFile, destFile));
      assertTrue(destFile.exists());
      assertTrue(destFile.delete());
      assertTrue(dir.delete());
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testUnzipFileToSameFolderTwice() throws Exception {
    URL resourceAsUrl = IOUtils.getResourceAsUrl("testFolder.zip", getClass());
    File zipFile = new File(resourceAsUrl.toURI());
    File outputDir = newFile(TEST_DIRECTORY);

    for (int i = 0; i < 2; i++) {
      unzip(zipFile, outputDir);
      File testFile = new File(UNZIPPED_FILE_PATH);
      assertTrue(testFile.exists());
    }
  }

  @Test
  public void unzipsFileWithoutParentFolderEntry() throws Exception {
    final String resourceName = "dummy.xml";
    final String resourceAlias = "folder" + separator + resourceName;
    final File compressedFile = new File(toDir, "test.zip");
    compress(compressedFile, new ZipResource[] {new ZipResource(resourceName, resourceAlias)});

    unzip(compressedFile, toDir);

    assertThat(new File(new File(toDir, "folder"), resourceName).exists(), is(true));
  }

  @Test
  public void doesNotUnzipAbsolutePaths() throws Exception {
    final String resourceName = "dummy.xml";
    final String resourceAlias = new File(resourceName).getAbsolutePath();
    final File compressedFile = new File(toDir, "test.zip");
    compress(compressedFile, new ZipResource[] {
        new ZipResource(resourceName, resourceName),
        new ZipResource(resourceName, resourceAlias)
    });

    try {
      thrownException.expect(InvalidZipFileException.class);
      thrownException.expectMessage("Absolute paths are not allowed: " + resourceAlias);
      unzip(compressedFile, toDir);
    } finally {
      // make sure it did not extract other archive files
      assertThat(new File(toDir, resourceName).exists(), is(false));
    }
  }

  @Test
  public void doesNotUnzipExternalPaths() throws Exception {
    final String resourceName = "dummy.xml";
    final String resourceAlias = Paths.get("folder", "..", "..", resourceName).toString();
    final File compressedFile = new File(toDir, "test.zip");
    compress(compressedFile, new ZipResource[] {
        new ZipResource(resourceName, resourceName),
        new ZipResource(resourceName, resourceAlias)
    });

    try {
      thrownException.expect(InvalidZipFileException.class);
      thrownException.expectMessage("External paths are not allowed: " + resourceAlias);
      unzip(compressedFile, toDir);
    } finally {
      // make sure it did not extract other archive files
      assertThat(new File(toDir, resourceName).exists(), is(false));
    }
  }

  @Test
  public void testUnzipStreamToSameFolderTwice() throws Exception {
    URL resourceAsUrl = IOUtils.getResourceAsUrl("testFolder.zip", getClass());
    File zipFile = new File(resourceAsUrl.toURI());
    File outputDir = newFile(TEST_DIRECTORY);

    for (int i = 0; i < 2; i++) {
      unzip(new FileInputStream(zipFile), outputDir);
      File testFile = new File(UNZIPPED_FILE_PATH);
      assertTrue(testFile.exists());
    }
  }

  @Test
  public void unzipsStreamWithoutParentFolderEntry() throws Exception {
    final String resourceName = "dummy.xml";
    final String resourceAlias = "folder" + separator + resourceName;
    final File compressedFile = new File(toDir, "test.zip");
    compress(compressedFile, new ZipResource[] {new ZipResource(resourceName, resourceAlias)});

    unzip(new FileInputStream(compressedFile), toDir);

    assertThat(new File(new File(toDir, "folder"), resourceName).exists(), is(true));
  }

  @Test
  public void unzipsStreamFailInMiddleCleanup() throws Exception {
    final String resourceName = "dummy.xml";
    final String resourceAlias = "folder" + separator + resourceName;
    final File compressedFile = new File(toDir, "test.zip");
    compress(compressedFile, new ZipResource[] {
        new ZipResource(resourceName, resourceAlias),
        new ZipResource(resourceName, "\0" + resourceAlias)});

    try {
      thrownException.expect(InvalidPathException.class);
      unzip(new FileInputStream(compressedFile), toDir);
    } finally {
      // make sure the file for the first entry was removed
      assertThat(new File(new File(toDir, "folder"), resourceName).exists(), is(false));
    }
  }

  @Test
  public void doesNotUnzipAbsolutePathsFromStream() throws Exception {
    final String resourceName = "dummy.xml";
    final String resourceAlias = new File(resourceName).getAbsolutePath();
    final File compressedFile = new File(toDir, "test.zip");
    compress(compressedFile, new ZipResource[] {
        new ZipResource(resourceName, resourceName),
        new ZipResource(resourceName, resourceAlias)
    });

    try {
      thrownException.expect(InvalidZipFileException.class);
      thrownException.expectMessage("Absolute paths are not allowed: " + resourceAlias);
      unzip(new FileInputStream(compressedFile), toDir);
    } finally {
      // make sure it did not extract other archive files
      assertThat(new File(toDir, resourceName).exists(), is(false));
    }
  }

  @Test
  public void doesNotUnzipExternalPathsFromStream() throws Exception {
    final String resourceName = "dummy.xml";
    final String resourceAlias = Paths.get("folder", "..", "..", resourceName).toString();
    final File compressedFile = new File(toDir, "test.zip");
    compress(compressedFile, new ZipResource[] {
        new ZipResource(resourceName, resourceName),
        new ZipResource(resourceName, resourceAlias)
    });

    try {
      thrownException.expect(InvalidZipFileException.class);
      thrownException.expectMessage("External paths are not allowed: " + resourceAlias);
      unzip(new FileInputStream(compressedFile), toDir);
    } finally {
      // make sure it did not extract other archive files
      assertThat(new File(toDir, resourceName).exists(), is(false));
    }
  }

  private File createTestFile(String filePath) throws IOException {
    return File.createTempFile(filePath, ".junit");
  }

  private File createTestDir(String dirPath) throws IOException {
    File file = createTestFile(dirPath);
    file.delete();
    file.mkdir();
    return file;
  }
}
