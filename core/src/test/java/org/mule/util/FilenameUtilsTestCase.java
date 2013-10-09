/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.File;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@SmallTest
public class FilenameUtilsTestCase extends AbstractMuleTestCase
{

    @Test
    public void testFileWithPathComponentsNullParameter()
    {
        File result = FilenameUtils.fileWithPathComponents(null);
        assertNull(result);
    }

    @Test
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

    @Test
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
