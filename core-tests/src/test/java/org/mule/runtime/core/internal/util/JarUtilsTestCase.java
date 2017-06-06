/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.junit.Test;

/**
 * Test suite for jar utilities.
 */
public class JarUtilsTestCase extends AbstractMuleTestCase {

  /**
   * <ol>
   * <li>Create jar file with all supported entries</li>
   * <li>Append jar file with additional entry</li>
   * <li>Read jar file and compare against previous entries written</li>
   * </ol>
   */
  @Test
  public void testCreateAppendReadJarFileEntries() {
    File jarFile = null;
    File jarEntryFile = null;

    try {
      // Create jar file from scratch
      String jarEntryString = "testString";
      jarEntryFile = File.createTempFile("test", "file");
      byte[] jarEntryBytes = jarEntryString.getBytes();

      LinkedHashMap jarEntries = new LinkedHashMap();
      jarEntries.put("META-INF/string", jarEntryString);
      jarEntries.put("META-INF/file", jarEntryFile);
      jarEntries.put("META-INF/byte", jarEntryBytes);

      jarFile = File.createTempFile("test", ".jar");
      assertTrue(jarFile.delete());

      JarUtils.createJarFileEntries(jarFile, jarEntries);

      // Append entry to jar file
      LinkedHashMap additionalJarEntries = new LinkedHashMap();
      additionalJarEntries.put("META-INF/append/string", jarEntryString);
      jarEntries.put("META-INF/append/string", jarEntryString);
      JarUtils.appendJarFileEntries(jarFile, additionalJarEntries);

      // Read jar file and verify previously written values
      LinkedHashMap readJarEntries = JarUtils.readJarFileEntries(jarFile);

      assertEquals(jarEntries.size(), readJarEntries.size());

      Iterator jarEntryIter = jarEntries.keySet().iterator();
      Iterator readJarEntryIter = readJarEntries.keySet().iterator();

      // Iterate through original and read jar entries, which must be equal.
      while (jarEntryIter.hasNext()) {
        String jarEntryPath = (String) jarEntryIter.next();
        String readJarEntryPath = (String) readJarEntryIter.next();

        assertNotNull(jarEntryPath);
        assertNotNull(readJarEntryPath);
        assertEquals(jarEntryPath, readJarEntryPath);

        Object jarEntry = jarEntries.get(jarEntryPath);
        Object readJarEntry = jarEntries.get(readJarEntryPath);

        if (jarEntry instanceof String || jarEntry instanceof File) {
          assertEquals(jarEntry, readJarEntry);
        } else if (jarEntry instanceof byte[]) {
          assertTrue(Arrays.equals((byte[]) jarEntry, (byte[]) readJarEntry));
        } else {
          fail("Unsupported jar entry read for " + jarEntryPath);
        }
      }
    } catch (Exception e) {
      fail(e.getMessage());
    } finally {
      if (jarFile != null) {
        jarFile.delete();
      }
      if (jarEntryFile != null) {
        jarEntryFile.delete();
      }
    }
  }
}
