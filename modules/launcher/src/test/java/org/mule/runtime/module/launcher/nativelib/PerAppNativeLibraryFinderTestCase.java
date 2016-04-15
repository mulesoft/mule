/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher.nativelib;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeThat;
import org.mule.tck.size.SmallTest;

import java.io.File;

import org.junit.Test;

@SmallTest
public class PerAppNativeLibraryFinderTestCase extends AbstractNativeLibraryFinderTestCase
{

    @Test
    public void returnsNullWhenLibraryNotFound() throws Exception
    {
        PerAppNativeLibraryFinder nativeLibraryFinder = new PerAppNativeLibraryFinder(libFolder.getRoot());

        String testLibPath = nativeLibraryFinder.findLibrary(TEST_LIB_NAME, null);

        assertThat(testLibPath, is(nullValue()));
    }

    @Test
    public void returnsParentLocalLibrary() throws Exception
    {
        PerAppNativeLibraryFinder nativeLibraryFinder = new PerAppNativeLibraryFinder(libFolder.getRoot());

        final String parentLibraryPath = "parent library path";
        String testLibPath = nativeLibraryFinder.findLibrary(TEST_LIB_NAME, parentLibraryPath);

        assertThat(testLibPath, equalTo(parentLibraryPath));
    }

    @Test
    public void findsLocalLibrary() throws Exception
    {
        File libraryFile = createDefaultNativeLibraryFile(TEST_LIB_NAME);

        PerAppNativeLibraryFinder nativeLibraryFinder = new PerAppNativeLibraryFinder(libFolder.getRoot());

        String testLibPath = nativeLibraryFinder.findLibrary(TEST_LIB_NAME, null);

        assertThat(testLibPath, equalTo(libraryFile.getAbsolutePath()));
    }

    @Test
    public void findsJnilibInMac() throws Exception
    {
        assumeThat(this, new MacOsMatcher());

        String libraryFileName = getJniLibFileName();

        File libraryFile = createNativeLibraryFile(libFolder.getRoot(), libraryFileName);

        PerAppNativeLibraryFinder nativeLibraryFinder = new PerAppNativeLibraryFinder(libFolder.getRoot());

        String testLibPath = nativeLibraryFinder.findLibrary(TEST_LIB_NAME, null);

        assertThat(testLibPath, equalTo(libraryFile.getAbsolutePath()));
    }
}
