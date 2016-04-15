/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher.nativelib;

import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class AbstractNativeLibraryFinderTestCase extends AbstractMuleTestCase
{

    public static final String TEST_LIB_NAME = "test";

    @Rule
    public TemporaryFolder libFolder = new TemporaryFolder();

    protected File createDefaultNativeLibraryFile(String libName) throws IOException
    {
        return createNativeLibraryFile(libFolder.getRoot(), System.mapLibraryName(libName));
    }

    protected File createNativeLibraryFile(File folder, String libFileName) throws IOException
    {

        File libraryFile = new File(folder, libFileName);
        FileUtils.write(libraryFile, "SOME.NATIVE.CODE");

        return libraryFile;
    }

    protected String getJniLibFileName()
    {
        String libraryFileName = System.mapLibraryName(TEST_LIB_NAME);
        int index = libraryFileName.lastIndexOf(".");
        libraryFileName = libraryFileName.substring(0, index) + PerAppNativeLibraryFinder.JNILIB_EXTENSION;
        return libraryFileName;
    }
}
