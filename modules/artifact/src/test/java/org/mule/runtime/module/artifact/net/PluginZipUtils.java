/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.net;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.rules.TemporaryFolder;

/**
 * helper class to zip a folder when testing
 *
 * @since 4.0
 */
public class PluginZipUtils {

  /**
   * Returns a file pointing to a dynamic generated zip file, generated from the folderPath
   * @param folderPath
   * @param folder place where the zip file should be created
   * @return a file pointing to a zip
   */
  public static File zipDirectory(String folderPath, TemporaryFolder folder, String pluginFolderName) {
    try {
      File dir = new File(folderPath);
      File zipFile = folder.newFile(pluginFolderName.concat(".zip"));
      FileOutputStream fout = new FileOutputStream(zipFile);
      ZipOutputStream zout = new ZipOutputStream(fout);
      zipSubDirectory("", dir, zout);
      zout.close();
      return zipFile;
    } catch (IOException e) {
      throw new IllegalArgumentException(String.format("There was a problem setting up for the folder [%s]", folderPath), e);
    }
  }

  private static void zipSubDirectory(String basePath, File dir, ZipOutputStream zout) throws IOException {
    byte[] buffer = new byte[4096];
    File[] files = dir.listFiles();
    for (File file : files) {
      if (file.isDirectory()) {
        String path = basePath + file.getName() + File.separator;
        zout.putNextEntry(new ZipEntry(path));
        zipSubDirectory(path, file, zout);
        zout.closeEntry();
      } else {
        FileInputStream fin = new FileInputStream(file);
        zout.putNextEntry(new ZipEntry(basePath + file.getName()));
        int length;
        while ((length = fin.read(buffer)) > 0) {
          zout.write(buffer, 0, length);
        }
        zout.closeEntry();
        fin.close();
      }
    }
  }
}
