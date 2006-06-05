/*
 * $Id:$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *  
 */

package org.mule.test.util;

import org.mule.util.FileUtils;

import java.io.File;

import junit.framework.TestCase;

/**
 * <p/> <code>FileUtilsTestCase</code> TODO (document class)
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision: 1665 $
 */
public class FileUtilsTestCase extends TestCase
{
    private final String TEST_FILE = "testFile.txt";

    public void testFileTools() throws Exception
    {
        File file = null;
        try {
            file = FileUtils.stringToFile(TEST_FILE, "this is a test file");
            assertNotNull(file);
            assertTrue(file.exists());

            file = FileUtils.stringToFile(TEST_FILE, " and this is appended content", true);

            String content = FileUtils.readFileToString(new File(TEST_FILE), null);

            assertNotNull(content);
            assertTrue(content.indexOf("this is a test file") > -1);
            assertTrue(content.indexOf(" and this is appended content") > -1);

            file = FileUtils.loadFile(TEST_FILE);
            assertNotNull(file);
            assertTrue(file.exists());

            file = FileUtils.createFile(TEST_FILE);
            assertNotNull(file);
            assertTrue(file.exists());

            file = FileUtils.createFile(TEST_FILE + "2");
            assertNotNull(file);
            assertTrue(file.exists());
            assertTrue(file.canRead());
            file.delete();

            file = FileUtils.loadFile(TEST_FILE);
            file.delete();

            File dir = FileUtils.openDirectory("src");
            assertNotNull(dir);
            assertTrue(dir.exists());
            assertTrue(dir.canRead());
            assertTrue(dir.isDirectory());

            dir = FileUtils.openDirectory("doesNotExist");
            assertNotNull(dir);
            assertTrue(dir.exists());
            assertTrue(dir.canRead());
            assertTrue(dir.isDirectory());
            dir.delete();

        }
        finally {
            if (file != null) {
                file.delete();
            }
        }
    }

    public void testFileNameTools() throws Exception
    {
        String filename = "Blah<Blah>.txt";
        String result = FileUtils.prepareWinFilename(filename);
        assertEquals("Blah(Blah).txt", result);

        filename = "Blah<Blah:a;b|c?d=e_f*g>.txt";
        result = FileUtils.prepareWinFilename(filename);
        assertEquals("Blah(Blah-a-b-c-d=e_f-g).txt", result);
    }

    public void testDirectoryTools() throws Exception
    {
        File dir = FileUtils.openDirectory("src");
        assertNotNull(dir);
        assertTrue(dir.exists());
        assertTrue(dir.canRead());
        assertTrue(dir.isDirectory());

        dir = FileUtils.openDirectory("doesNotExist");
        assertNotNull(dir);
        assertTrue(dir.exists());
        assertTrue(dir.canRead());
        assertTrue(dir.isDirectory());
        dir.delete();
    }

}
