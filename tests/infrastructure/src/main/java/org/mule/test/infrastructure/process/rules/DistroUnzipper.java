/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.process.rules;

import static org.apache.commons.io.FileUtils.copyInputStreamToFile;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;


public class DistroUnzipper {

  private File file;
  private File destDir;
  private File rootFile;

  public DistroUnzipper(File file, File destDir) {
    this.file = file;
    this.destDir = destDir;
  }

  public File muleHome() {
    return rootFile;
  }

  public DistroUnzipper unzip() throws IOException {
    try (ZipFile zip = new ZipFile(file)) {
      Enumeration<? extends ZipEntry> zipFileEntries = zip.entries();
      ZipEntry root = zipFileEntries.nextElement();
      rootFile = new File(destDir, root.getName());
      rootFile.mkdirs();
      chmodRwx(rootFile);
      while (zipFileEntries.hasMoreElements()) {
        ZipEntry entry = zipFileEntries.nextElement();
        File destFile = new File(destDir, entry.getName());
        if (entry.isDirectory()) {
          destFile.mkdir();
        } else {
          copyInputStreamToFile(zip.getInputStream(entry), destFile);
          chmodRwx(destFile);
        }
      }
      return this;
    }
  }

  private void chmodRwx(File destFile) {
    destFile.setExecutable(true, false);
    destFile.setWritable(true, false);
    destFile.setReadable(true, false);
  }
}
