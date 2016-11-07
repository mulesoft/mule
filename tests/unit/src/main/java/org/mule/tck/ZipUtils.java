/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck;

import static java.io.File.separator;
import static java.nio.file.FileVisitResult.*;
import static java.nio.file.Files.walkFileTree;
import static org.apache.commons.lang.StringUtils.removeStart;
import org.mule.runtime.core.util.ClassUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Provides utility methods to work with ZIP files
 */
public class ZipUtils {

  private ZipUtils() {}

  /**
   * Describes a resource that can be compressed in a ZIP file
   */
  public static class ZipResource {

    private final String file;
    private final String alias;

    public ZipResource(String file, String alias) {
      this.file = file;
      this.alias = alias;
    }
  }

  /**
   * Compress a set of resource files into a ZIP file
   *
   * @param targetFile file that will contain the zipped files
   * @param resources resources to compress
   * @throws UncheckedIOException in case of any error processing the files
   */
  public static void compress(File targetFile, ZipResource[] resources) {
    try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(targetFile))) {
      for (ZipResource zipResource : resources) {
        URL resourceUrl = ClassUtils.getResource(zipResource.file, ZipUtils.class);
        if (resourceUrl == null) {
          resourceUrl = new File(zipResource.file).toURI().toURL();
        }

        try (FileInputStream in = new FileInputStream(resourceUrl.getFile())) {
          out.putNextEntry(new ZipEntry(zipResource.alias == null ? zipResource.file : zipResource.alias));

          byte[] buffer = new byte[1024];

          int count;
          while ((count = in.read(buffer)) > 0) {
            out.write(buffer, 0, count);
          }
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Takes a source {@link Path} it goes over every file recursively generating a temporary array of {@link ZipResource}s
   * so that it can later compress it to the desired target.
   *
   * @param targetFile destination folder to compress the found files from {@code path}
   * @param path to walk, while assembling a list of files to be later compressed.
   */
  public static void compressDirectory(File targetFile, Path path) {
    List<ZipResource> resources = new ArrayList<>();
    try {
      walkFileTree(path, new SimpleFileVisitor<Path>() {

        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          resources.add(new ZipResource(file.toString(), removeStart(file.toString(), path.toString() + separator)));
          return CONTINUE;
        }
      });
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    compress(targetFile, resources.toArray(new ZipResource[resources.size()]));
  }
}
