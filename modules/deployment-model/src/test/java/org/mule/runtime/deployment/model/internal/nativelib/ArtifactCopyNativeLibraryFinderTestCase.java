/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.internal.nativelib;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assume.assumeThat;
import static org.mule.runtime.deployment.model.internal.nativelib.ArtifactCopyNativeLibraryFinder.JNILIB_EXTENSION;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SmallTest
public class ArtifactCopyNativeLibraryFinderTestCase extends AbstractMuleTestCase {

  public static final String TEST_LIB_NAME = "test";

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Rule
  public TemporaryFolder libFolder = new TemporaryFolder();

  @Test
  public void createsTempFolder() throws Exception {
    tempFolder.getRoot().delete();

    new ArtifactCopyNativeLibraryFinder(tempFolder.getRoot(), new URL[0]);

    assertThat(tempFolder.getRoot().exists(), equalTo(true));
  }

  @Test
  public void cleansTempFolder() throws Exception {
    File nativeLibraryTempFolder = new File(tempFolder.getRoot(), "native");
    assertThat(nativeLibraryTempFolder.mkdirs(), is(true));

    File libraryFile = createNativeLibraryFile(nativeLibraryTempFolder, "tempfile.jar");

    new ArtifactCopyNativeLibraryFinder(tempFolder.getRoot(), new URL[] {libraryFile.toURL()});

    assertThat(libraryFile.exists(), equalTo(false));
  }

  @Test
  public void returnsNullWhenLibraryNotFound() throws Exception {
    NativeLibraryFinder nativeLibraryFinder = new ArtifactCopyNativeLibraryFinder(tempFolder.getRoot(), new URL[0]);

    String testLibPath = nativeLibraryFinder.findLibrary(TEST_LIB_NAME, null);

    assertThat(testLibPath, nullValue());
  }

  @Test
  public void findsLocalLibrary() throws Exception {
    File nativeLibrary = createDefaultNativeLibraryFile(TEST_LIB_NAME);

    NativeLibraryFinder nativeLibraryFinder =
        new ArtifactCopyNativeLibraryFinder(tempFolder.getRoot(), new URL[] {nativeLibrary.toURL()});

    String testLibPath = nativeLibraryFinder.findLibrary(TEST_LIB_NAME, null);

    assertThat(testLibPath, startsWith(tempFolder.getRoot().getAbsolutePath()));
    assertThat(testLibPath, containsString(TEST_LIB_NAME));
  }

  @Test
  public void findsJnilibInMac() throws Exception {
    assumeThat(this, new MacOsMatcher());

    String libraryFileName = getJniLibFileName();
    File nativeLibrary = createNativeLibraryFile(libFolder.getRoot(), libraryFileName);

    NativeLibraryFinder nativeLibraryFinder =
        new ArtifactCopyNativeLibraryFinder(tempFolder.getRoot(), new URL[] {nativeLibrary.toURL()});

    String testLibPath = nativeLibraryFinder.findLibrary(TEST_LIB_NAME, null);

    assertThat(testLibPath, startsWith(tempFolder.getRoot().getAbsolutePath()));
    assertThat(testLibPath, containsString(TEST_LIB_NAME));
  }

  private File createDefaultNativeLibraryFile(String libName) throws IOException {
    return createNativeLibraryFile(libFolder.getRoot(), System.mapLibraryName(libName));
  }

  private File createNativeLibraryFile(File folder, String libFileName) throws IOException {
    File libraryFile = new File(folder, libFileName);
    FileUtils.write(libraryFile, "SOME.NATIVE.CODE");

    return libraryFile;
  }

  private String getJniLibFileName() {
    String libraryFileName = System.mapLibraryName(TEST_LIB_NAME);
    int index = libraryFileName.lastIndexOf(".");
    libraryFileName = libraryFileName.substring(0, index) + JNILIB_EXTENSION;
    return libraryFileName;
  }
}
