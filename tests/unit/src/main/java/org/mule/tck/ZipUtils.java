/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.util.ClassUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
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

        try (FileInputStream in = new FileInputStream(new File(resourceUrl.toURI()))) {
          out.putNextEntry(new ZipEntry(zipResource.alias == null ? zipResource.file : zipResource.alias));

          byte[] buffer = new byte[1024];

          int count;
          while ((count = in.read(buffer)) > 0) {
            out.write(buffer, 0, count);
          }
        } catch (URISyntaxException e) {
          throw new MuleRuntimeException(e);
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
