/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import java.io.File;

import junit.framework.TestCase;

public class FilenameUtilsTestCase extends TestCase
{
    public void testFileWithPathComponentsNullParameter()
    {
        File result = FilenameUtils.fileWithPathComponents(null);
        assertNull(result);
    }

    public void testFileWithNullElements()
    {
        File tempDir = getBuidDirectory();
        File result = FilenameUtils.fileWithPathComponents(
                            new String[] {tempDir.getAbsolutePath(), "tmp", null, "bar"});

        // make sure that we can validate the test result on all platforms.
        String resultNormalized = result.getAbsolutePath().replace(File.separatorChar, '|');
        String excpected = tempDir.getAbsolutePath().replace(File.separatorChar, '|') + "|tmp|bar";
        assertEquals(excpected, resultNormalized);
    }

    public void testFileWithPathComponents()
    {
        String tempDirPath = getBuidDirectory().getAbsolutePath();

        File result = FilenameUtils.fileWithPathComponents(new String[]{tempDirPath, "tmp", "foo", "bar"});

        // make sure that we can validate the test result on all platforms.
        String resultNormalized = result.getAbsolutePath().replace(File.separatorChar, '|');
        String expected = tempDirPath.replace(File.separatorChar, '|') + "|tmp|foo|bar";
        assertEquals(expected, resultNormalized);
    }

    /**
     * Used to obtain base directory used in tests. Uses the build directory;
     * "target" in the current working directory.
     */
    private File getBuidDirectory()
    {
        return FileUtils.newFile(SystemUtils.getUserDir(), "target");
    }

}
