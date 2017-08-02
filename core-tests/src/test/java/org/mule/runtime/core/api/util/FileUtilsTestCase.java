/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mule.runtime.core.api.util.FileUtils.unzip;
import static org.mule.tck.ZipUtils.compress;

import org.mule.tck.ZipUtils.ZipResource;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileUtilsTestCase extends AbstractMuleTestCase {

  private static final String TEST_FILE = "testFile.txt";
  private static final String TEST_DIRECTORY = "target" + File.separator + "testDirectory";
  private static final String UNZIPPED_FILE_PATH =
      TEST_DIRECTORY + File.separator + "testFolder" + File.separator + "testFile.txt";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private File toDir;

  @Before
  public void setupDir() {
    toDir = temporaryFolder.getRoot();
  }

  @Test
  public void testFileTools() throws Exception {
    File file = null;
    try {
      file = FileUtils.stringToFile(TEST_FILE, "this is a test file");
      assertNotNull(file);
      assertTrue(file.exists());

      file = FileUtils.stringToFile(TEST_FILE, " and this is appended content", true);

      String content = readFileToString(FileUtils.newFile(TEST_FILE), (String) null);

      assertNotNull(content);
      assertTrue(content.indexOf("this is a test file") > -1);
      assertTrue(content.indexOf(" and this is appended content") > -1);

      file = FileUtils.newFile(TEST_FILE);
      assertNotNull(file);
      assertTrue(file.exists());

      file = FileUtils.createFile(TEST_FILE);
      assertNotNull(file);
      assertTrue(file.exists());

      file = FileUtils.createFile(TEST_FILE + "2");
      assertNotNull(file);
      assertTrue(file.exists());
      assertTrue(file.canRead());
      file.delete();

      file = FileUtils.newFile(TEST_FILE);
      file.delete();

      File dir = FileUtils.openDirectory("src");
      assertNotNull(dir);
      assertTrue(dir.exists());
      assertTrue(dir.canRead());
      assertTrue(dir.isDirectory());

      dir = FileUtils.openDirectory("doesNotExist");
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
    String result = FileUtils.prepareWinFilename(filename);
    assertEquals("Blah(Blah).txt", result);

    filename = "Bla]h<Blah:a;b|c?d=e_f*g>.txt";
    result = FileUtils.prepareWinFilename(filename);
    assertEquals("Bla-h(Blah-a-b-c-d=e_f-g).txt", result);

    filename = "B\"la-h<Blah:a;b|c?d=e_f*g>.txt";
    result = FileUtils.prepareWinFilename(filename);
    assertEquals("B-la-h(Blah-a-b-c-d=e_f-g).txt", result);
  }

  @Test
  public void testDirectoryTools() throws Exception {
    File dir = FileUtils.openDirectory("src");
    assertNotNull(dir);
    assertTrue(dir.exists());
    assertTrue(dir.canRead());
    assertTrue(dir.isDirectory());

    dir = FileUtils.openDirectory("doesNotExist");
    assertNotNull(dir);
    assertTrue(dir.exists());
    assertTrue(dir.canRead());
    assertTrue(dir.isDirectory());
    FileUtils.deleteTree(dir);
  }

  @Test
  public void testExtractResource() throws Exception {
    String testDir = TEST_DIRECTORY + File.separator + "Test-1";
    File outputDir = FileUtils.newFile(testDir);
    if (!outputDir.exists()) {
      assertTrue("Failed to create output dirs.", outputDir.mkdirs());
    }
    String res = "META-INF/MANIFEST.MF";
    FileUtils.extractResources(res, getClass(), outputDir, true);
    File result = FileUtils.newFile(testDir, res);
    assertNotNull(result);
    assertTrue(result.exists());
    assertTrue(result.canRead());
    assertTrue(result.isFile());
    assertTrue(result.length() > 0);
    FileUtils.deleteTree(outputDir);
  }

  @Test
  public void testExtractResources() throws Exception {
    String testDir = TEST_DIRECTORY + File.separator + "Test-2";
    File outputDir = FileUtils.newFile(testDir);
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }
    String res = "META-INF/";
    FileUtils.extractResources(res, getClass(), outputDir, true);
    File result = FileUtils.newFile(testDir, res);
    assertNotNull(result);
    assertTrue(result.exists());
    assertTrue(result.canRead());
    assertTrue(result.isDirectory());
    FileUtils.deleteTree(outputDir);
  }

  @Test
  public void testExtractFileResource() throws Exception {
    String testDir = TEST_DIRECTORY + File.separator + "Test-3";
    File outputDir = FileUtils.newFile(testDir);
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }
    String res = "org/mule/runtime/core/api/util/FileUtils.class";
    FileUtils.extractResources(res, FileUtils.class, outputDir, true);
    File result = FileUtils.newFile(testDir, res);
    assertNotNull(result);
    assertTrue(result.exists());
    assertTrue(result.canRead());
    assertTrue(result.isFile());
    assertTrue(result.length() > 0);
    FileUtils.deleteTree(outputDir);
  }

  @Test
  public void testExtractFileResources() throws Exception {
    String testDir = TEST_DIRECTORY + File.separator + "Test-4";
    File outputDir = FileUtils.newFile(testDir);
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }
    String res = "org/mule/runtime/core/api/util/";
    FileUtils.extractResources(res, FileUtils.class, outputDir, true);
    File result = FileUtils.newFile(testDir, res);
    assertNotNull(result);
    assertTrue(result.exists());
    assertTrue(result.canRead());
    assertTrue(result.isDirectory());
    FileUtils.deleteTree(outputDir);
  }

  @Test
  public void testExtractResourceWithoutKeepingDirStructure() throws Exception {
    String testDir = TEST_DIRECTORY + File.separator + "Test-5";
    File outputDir = FileUtils.newFile(testDir);
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }
    String fileName = "MANIFEST.MF";
    String res = "META-INF/" + fileName;
    FileUtils.extractResources(res, getClass(), outputDir, false);
    File result = FileUtils.newFile(testDir, fileName);
    assertNotNull(result);
    assertTrue(result.exists());
    assertTrue(result.canRead());
    assertTrue(result.isFile());
    assertTrue(result.length() > 0);
    FileUtils.deleteTree(outputDir);
  }

  @Test
  public void testExtractResourcesWithoutKeepingDirStructure() throws Exception {
    String testDir = TEST_DIRECTORY + File.separator + "Test-6";
    File outputDir = FileUtils.newFile(testDir);
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }
    String fileName = "util/FileUtilsTestCase.class";
    String res = "org/mule/runtime/core/api";
    FileUtils.extractResources(res, FileUtilsTestCase.class, outputDir, false);
    File result = FileUtils.newFile(testDir, fileName);
    assertNotNull(result);
    assertTrue(result.exists());
    assertTrue(result.canRead());
    assertTrue(result.isFile());
    assertTrue(result.length() > 0);
    FileUtils.deleteTree(outputDir);
  }

  @Test
  public void testExtractFileResourceWithoutKeepingDirStructure() throws Exception {
    String testDir = TEST_DIRECTORY + File.separator + "Test-7";
    File outputDir = FileUtils.newFile(testDir);
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }
    String fileName = "FileUtils.class";
    String res = "org/mule/runtime/core/api/util/" + fileName;
    FileUtils.extractResources(res, FileUtils.class, outputDir, false);
    File result = FileUtils.newFile(testDir, fileName);
    assertNotNull(result);
    assertTrue(result.exists());
    assertTrue(result.canRead());
    assertTrue(result.isFile());
    assertTrue(result.length() > 0);
    FileUtils.deleteTree(outputDir);
  }

  @Test
  public void testExtractFileResourcesWithoutKeepingDirStructure() throws Exception {
    String testDir = TEST_DIRECTORY + File.separator + "Test-8";
    File outputDir = FileUtils.newFile(testDir);
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }
    String fileName = "util/FileUtilsTestCase.class";
    String res = "org/mule/runtime/core/api/";
    FileUtils.extractResources(res, FileUtilsTestCase.class, outputDir, false);
    File result = FileUtils.newFile(testDir, fileName);
    assertNotNull(result);
    assertTrue(result.exists());
    assertTrue(result.canRead());
    assertTrue(result.isFile());
    assertTrue(result.length() > 0);
    FileUtils.deleteTree(outputDir);
  }

  @Test
  public void testDeleteTreeWithIgnoredDirectories() throws Exception {
    final String testDir = TEST_DIRECTORY + File.separator + "Test-deleting";
    File outputDir = FileUtils.newFile(testDir);
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }

    File toBeDeleted1 = FileUtils.newFile(outputDir, "toBeDeleted1/");
    toBeDeleted1.mkdirs();
    File toBeDeleted2 = FileUtils.newFile(outputDir, "toBeDeleted2/");
    toBeDeleted2.mkdirs();

    File keepMeIntact = FileUtils.newFile(outputDir, "keepMeIntact/");
    keepMeIntact.mkdirs();

    FileUtils.deleteTree(outputDir, new String[] {"keepMeIntact"});

    assertTrue("Shouldn't have been deleted.", keepMeIntact.exists());

    FileUtils.deleteTree(outputDir);
  }

  @Test
  public void testRenameFile() {
    try {
      File sourceFile = createTestFile("source");
      File destFile = createTestFile("dest");

      assertTrue(destFile.delete());
      assertTrue(FileUtils.renameFile(sourceFile, destFile));
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

      assertTrue(FileUtils.renameFile(sourceFile, destFile));
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
    File outputDir = FileUtils.newFile(TEST_DIRECTORY);

    for (int i = 0; i < 2; i++) {
      unzip(zipFile, outputDir);
      File testFile = new File(UNZIPPED_FILE_PATH);
      assertTrue(testFile.exists());
    }
  }

  @Test
  public void unzipsFileWithoutParentFolderEntry() throws Exception {
    final String resourceName = "dummy.xml";
    final String resourceAlias = "folder" + File.separator + resourceName;
    final File compressedFile = new File(toDir, "test.zip");
    compress(compressedFile, new ZipResource[] {new ZipResource(resourceName, resourceAlias)});

    unzip(compressedFile, toDir);

    assertThat(new File(new File(toDir, "folder"), resourceName).exists(), is(true));
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
