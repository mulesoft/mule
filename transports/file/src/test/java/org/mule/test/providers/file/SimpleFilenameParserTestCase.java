/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.providers.file;

import org.mule.providers.DefaultMessageAdapter;
import org.mule.providers.file.FileConnector;
import org.mule.providers.file.SimpleFilenameParser;
import org.mule.tck.AbstractMuleTestCase;

/**
 * Test the syntax of the SimpleFilename parser
 */
public class SimpleFilenameParserTestCase extends AbstractMuleTestCase
{

    public void testAntStyleParsing()
    {
        SimpleFilenameParser parser = new SimpleFilenameParser();
        DefaultMessageAdapter adapter = new DefaultMessageAdapter("hello");
        adapter.setProperty("foo", "bar");
        adapter.setProperty(FileConnector.PROPERTY_FILENAME, "blah");
        String result = parser.getFilename(adapter, "Test1_${COUNT}.txt");
        assertEquals("Test1_0.txt", result);

        result = parser.getFilename(adapter, "Test2_${DATE:yyMMdd}.txt");
        assertEquals(16, result.length());

        result = parser.getFilename(adapter, "Test3_${DATE}.txt");
        assertEquals(31, result.length());

        result = parser.getFilename(adapter, "Test4_${SYSTIME}.txt");
        assertFalse(result.equals("Test4_${SYSTIME}.txt"));

        result = parser.getFilename(adapter, "Test5_${UUID}.txt");
        assertFalse(result.equals("Test5_${UUID}.txt"));

        result = parser.getFilename(adapter, "Test6_${COUNT}.txt");
        assertEquals("Test6_1.txt", result);

        result = parser.getFilename(adapter, "Test7_${ORIGINALNAME}.txt");
        assertEquals("Test7_blah.txt", result);

        result = parser.getFilename(adapter, "Test8_${foo}.txt");
        assertEquals("Test8_bar.txt", result);

        result = parser.getFilename(adapter, "Test9_${xxx}.txt");
        assertEquals("Test9_${xxx}.txt", result);

    }

    public void testSquareStyleParsing()
    {
        SimpleFilenameParser parser = new SimpleFilenameParser();
        DefaultMessageAdapter adapter = new DefaultMessageAdapter("hello");
        adapter.setProperty("foo", "bar");
        adapter.setProperty(FileConnector.PROPERTY_FILENAME, "blah");
        String result = parser.getFilename(adapter, "Test1_[COUNT].txt");
        assertEquals("Test1_0.txt", result);

        result = parser.getFilename(adapter, "Test2_[DATE:yyMMdd].txt");
        assertEquals("got result: " + result, 16, result.length());

        result = parser.getFilename(adapter, "Test3_[DATE].txt");
        assertEquals("got result: '" + result, 31, result.length());

        result = parser.getFilename(adapter, "Test4_[SYSTIME].txt");
        assertFalse(result.equals("Test4_[SYSTIME].txt"));

        result = parser.getFilename(adapter, "Test5_[UUID].txt");
        assertFalse(result.equals("Test5_[UUID].txt"));

        result = parser.getFilename(adapter, "Test6_[COUNT].txt");
        assertEquals("Test6_1.txt", result);

        result = parser.getFilename(adapter, "Test7_[ORIGINALNAME].txt");
        assertEquals("Test7_blah.txt", result);

        result = parser.getFilename(adapter, "Test8_[foo].txt");
        assertEquals("Test8_bar.txt", result);

        result = parser.getFilename(adapter, "Test9_[xxx].txt");
        assertEquals("Test9_[xxx].txt", result);
    }

}
