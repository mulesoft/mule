/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher.nativelib;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.mule.util.FileUtils.deleteFile;
import org.mule.tck.size.SmallTest;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SmallTest
public class PerAppCopyNativeLibraryFinderTestCase extends AbstractNativeLibraryFinderTestCase
{

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();


    @Test
    public void createsTempFolder() throws Exception
    {
        deleteFile(tempFolder.getRoot());

        new PerAppCopyNativeLibraryFinder(libFolder.getRoot(), tempFolder.getRoot());

        assertThat(tempFolder.getRoot().exists(), equalTo(true));
    }

    @Test
    public void cleansTempFolder() throws Exception
    {
        File libraryFile = createNativeLibraryFile(tempFolder.getRoot(), "tempfile.jar");

        new PerAppCopyNativeLibraryFinder(libFolder.getRoot(), tempFolder.getRoot());

        assertThat(libraryFile.exists(), equalTo(false));
    }

    @Test
    public void returnsNullWhenLibraryNotFound() throws Exception
    {
        NativeLibraryFinder nativeLibraryFinder = new PerAppCopyNativeLibraryFinder(libFolder.getRoot(), tempFolder.getRoot());

        String testLibPath = nativeLibraryFinder.findLibrary(TEST_LIB_NAME, null);

        assertThat(testLibPath, is(nullValue()));
    }

    @Test
    public void returnsParentLocalLibrary() throws Exception
    {
        File parentNativeLibrary = createDefaultNativeLibraryFile(TEST_LIB_NAME);

        NativeLibraryFinder nativeLibraryFinder = new PerAppCopyNativeLibraryFinder(libFolder.getRoot(), tempFolder.getRoot());

        String testLibPath = nativeLibraryFinder.findLibrary(TEST_LIB_NAME, parentNativeLibrary.getAbsolutePath());

        assertThat(testLibPath, startsWith(tempFolder.getRoot().getAbsolutePath()));
        assertThat(testLibPath, containsString(TEST_LIB_NAME));
    }

    @Test
    public void findsLocalLibrary() throws Exception
    {
        createDefaultNativeLibraryFile(TEST_LIB_NAME);

        NativeLibraryFinder nativeLibraryFinder = new PerAppCopyNativeLibraryFinder(libFolder.getRoot(), tempFolder.getRoot());

        String testLibPath = nativeLibraryFinder.findLibrary(TEST_LIB_NAME, null);

        assertThat(testLibPath, startsWith(tempFolder.getRoot().getAbsolutePath()));
        assertThat(testLibPath, containsString(TEST_LIB_NAME));
    }

    @Test
    public void findsJnilibInMac() throws Exception
    {
        assumeThat(this, new MacOsMatcher());

        String libraryFileName = getJniLibFileName();

        createNativeLibraryFile(libFolder.getRoot(), libraryFileName);

        NativeLibraryFinder nativeLibraryFinder = new PerAppCopyNativeLibraryFinder(libFolder.getRoot(), tempFolder.getRoot());

        String testLibPath = nativeLibraryFinder.findLibrary(TEST_LIB_NAME, null);

        assertThat(testLibPath, startsWith(tempFolder.getRoot().getAbsolutePath()));
        assertThat(testLibPath, containsString(TEST_LIB_NAME));
    }
}
