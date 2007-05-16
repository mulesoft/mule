/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import java.io.File;

import junit.framework.Assert;
import junit.framework.TestCase;

public class FilenameUtilsTestCase extends TestCase
{
    public void testFileWithPathComponentsNullParameter()
    {
        File result = FilenameUtils.fileWithPathComponents((String[])null);
        Assert.assertNull(result);
    }
    
    public void testFileWithNullElements()
    {
        // returns '/' on Unix, 'c:\' or similar on windows
        File root = File.listRoots()[0];
        
        File result = FilenameUtils.fileWithPathComponents(
            new String[] {root.getAbsolutePath(), "tmp", null, "bar"});

        // make sure that we can validate the test result on all platforms.
        String resultNormalized = result.getAbsolutePath().replace(File.separatorChar, '|');
        String excpected = root.getAbsolutePath().replace(File.separatorChar, '|') + "tmp|bar";
        Assert.assertEquals(excpected, resultNormalized);
    }
    
    public void testFileWithPathComponents()
    {
        // returns '/' on Unix, 'c:\' or similar on windows
        String root = File.listRoots()[0].getAbsolutePath();

        File result = FilenameUtils.fileWithPathComponents(new String[] {root, "tmp", "foo", "bar"});
        
        // make sure that we can validate the test result on all platforms.
        String resultNormalized = result.getAbsolutePath().replace(File.separatorChar, '|');
        String expected = root.replace(File.separatorChar, '|') + "tmp|foo|bar";
        Assert.assertEquals(expected, resultNormalized);
    }
}


