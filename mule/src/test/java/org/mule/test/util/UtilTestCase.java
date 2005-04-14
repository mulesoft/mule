/*
 * $Header$ $Revision$ $Date$
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

import junit.framework.TestCase;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.util.Utility;

import java.io.File;
import java.util.Date;

/**
 * <p/>
 * <code>UtilTestCase</code> TODO (document class)
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class UtilTestCase extends TestCase
{
    private final String TEST_FILE = "testFile.txt";
    private final String TEST_DATE_FORMAT = "dd/MM/yyyy hh:mm:ss";
    private final String TEST_DATE_FORMAT_2 = "dd-MM-yy, hh:mm";


    public void testByteTools() throws Exception
    {
        Orange orange = new Orange();
        orange.setBrand("Juicy");
        orange.setRadius(new Double(2.2));
        orange.setSegments(new Integer(10));

        byte[] src = Utility.objectToByteArray(orange);
        assertNotNull(src);
        assertTrue(src.length > 0);

        Object result = Utility.byteArrayToObject(src);
        assertNotNull(result);
        assertTrue(result instanceof Orange);

        Orange newOrange = (Orange) result;

        assertEquals(new Double(2.2), newOrange.getRadius());
        assertEquals(new Integer(10), newOrange.getSegments());
        assertEquals("Juicy", newOrange.getBrand());
    }

    public void testFileTools() throws Exception
    {
        File file = null;
        try
        {
            file = Utility.stringToFile(TEST_FILE, "this is a test file");
            assertNotNull(file);
            assertTrue(file.exists());

            file = Utility.stringToFile(TEST_FILE, " and this is appended content", true);

            String content = Utility.fileToString(TEST_FILE);

            assertNotNull(content);
            assertTrue(content.indexOf("this is a test file") > -1);
            assertTrue(content.indexOf(" and this is appended content") > -1);

            file = Utility.loadFile(TEST_FILE);
            assertNotNull(file);
            assertTrue(file.exists());

            file = Utility.createFile(TEST_FILE);
            assertNotNull(file);
            assertTrue(file.exists());

            file = Utility.createFile(TEST_FILE + "2");
            assertNotNull(file);
            assertTrue(file.exists());
            assertTrue(file.canRead());
            file.delete();

            file = Utility.loadFile(TEST_FILE);
            file.delete();

            File dir = Utility.openDirectory("src");
            assertNotNull(dir);
            assertTrue(dir.exists());
            assertTrue(dir.canRead());
            assertTrue(dir.isDirectory());

            dir = Utility.openDirectory("doesNotExist");
            assertNotNull(dir);
            assertTrue(dir.exists());
            assertTrue(dir.canRead());
            assertTrue(dir.isDirectory());
            dir.delete();

        }
        finally
        {
            if (file != null)
                file.delete();
        }
    }

    public void testFileNameTools() throws Exception
    {
        String filename = "Blah<Blah>.txt";
        String result = Utility.prepareWinFilename(filename);
        assertEquals("Blah(Blah).txt", result);

        filename = "Blah<Blah:a;b|c?d=e_f*g>.txt";
        result = Utility.prepareWinFilename(filename);
        assertEquals("Blah(Blah-a-b-c-d=e_f-g).txt", result);
    }

    public void testDirectoryTools() throws Exception
    {
        File dir = Utility.openDirectory("src");
        assertNotNull(dir);
        assertTrue(dir.exists());
        assertTrue(dir.canRead());
        assertTrue(dir.isDirectory());

        dir = Utility.openDirectory("doesNotExist");
        assertNotNull(dir);
        assertTrue(dir.exists());
        assertTrue(dir.canRead());
        assertTrue(dir.isDirectory());
        dir.delete();
    }

    public void testConvertTools() throws Exception
    {
        assertTrue(Utility.getBooleanValue("true"));
        assertTrue(!Utility.getBooleanValue("false"));
        assertTrue(!Utility.getBooleanValue("blah"));
    }

    public void testDateUtils() throws Exception
    {
        String date = "12/11/2002 12:06:47";

        Date result = Utility.getDateFromString(date, TEST_DATE_FORMAT);

        assertTrue(result.before(new Date(System.currentTimeMillis())));

        String newDate = Utility.getStringFromDate(result, TEST_DATE_FORMAT);

        assertEquals(date, newDate);

        String timestamp = Utility.formatTimeStamp(result, TEST_DATE_FORMAT_2);

        assertEquals("12-11-02, 12:06", timestamp);

        String newTimestamp = Utility.getTimeStamp(TEST_DATE_FORMAT_2);

        assertEquals(Utility.getStringFromDate(new Date(), TEST_DATE_FORMAT_2), newTimestamp);

    }
}
