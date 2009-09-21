/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.file;

import org.mule.api.transport.MessageAdapter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transport.DefaultMessageAdapter;

/**
 * Test the syntax of the SimpleFilename parser
 */
public class ExpressionFilenameParserTestCase extends AbstractMuleTestCase
{
    private ExpressionFilenameParser parser;
    private MessageAdapter adapter;

    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        parser = new ExpressionFilenameParser();
        parser.setMuleContext(muleContext);

        adapter = new DefaultMessageAdapter("hello");
        adapter.setProperty("foo", "bar");
        adapter.setProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME, "originalName");
        adapter.setProperty(FileConnector.PROPERTY_FILENAME, "newName");
    }

    public void testWigglyMuleStyleParsing()
    {
        String result = parser.getFilename(adapter, "Test1_#[function:count].txt");
        assertEquals("Test1_0.txt", result);

        result = parser.getFilename(adapter, "Test2_#[function:datestamp-yyMMdd].txt");
        assertEquals(16, result.length());

        result = parser.getFilename(adapter, "Test3_#[function:datestamp].txt");
        assertEquals(31, result.length());

        result = parser.getFilename(adapter, "Test4_#[function:systime].txt");
        assertFalse(result.equals("Test4_#[function:systime].txt"));

        result = parser.getFilename(adapter, "Test5_#[function:uuid].txt");
        assertFalse(result.equals("Test5_#[function:uuid].txt"));

        result = parser.getFilename(adapter, "Test6_#[function:count].txt");
        assertEquals("Test6_1.txt", result);

        result = parser.getFilename(adapter, "Test7_#[header:originalFilename].txt");
        assertEquals("Test7_originalName.txt", result);

        result = parser.getFilename(adapter, "Test8_#[header:foo].txt");
        assertEquals("Test8_bar.txt", result);

        result = parser.getFilename(adapter, "Test9_#[header:xxx?].txt");
        assertEquals("Test9_#[header:xxx?].txt", result);

        try
        {
            result = parser.getFilename(adapter, "Test9_#[header:xxx].txt");
            fail("Property xxx is not available");
        }
        catch (Exception e)
        {
            //Expected
        }

    }

    public void testSquareStyleParsing()
    {
        String result = parser.getFilename(adapter, "Test1_[function:count].txt");
        assertEquals("Test1_0.txt", result);

        result = parser.getFilename(adapter, "Test2_[function:dateStamp-yyMMdd].txt");
        assertEquals("got result: " + result, 16, result.length());

        result = parser.getFilename(adapter, "Test3_[function:dateStamp].txt");
        assertEquals("got result: '" + result, 31, result.length());

        result = parser.getFilename(adapter, "Test4_[function:systime].txt");
        assertFalse(result.equals("Test4_[function:systime].txt"));

        result = parser.getFilename(adapter, "Test5_[function:uuid].txt");
        assertFalse(result.equals("Test5_[function:uuid].txt"));

        result = parser.getFilename(adapter, "Test6_[function:count].txt");
        assertEquals("Test6_1.txt", result);

        result = parser.getFilename(adapter, "Test7_[header:originalFilename].txt");
        assertEquals("Test7_originalName.txt", result);

        result = parser.getFilename(adapter, "Test8_[header:foo].txt");
        assertEquals("Test8_bar.txt", result);

        try
        {
            result = parser.getFilename(adapter, "Test9_[header:xxx].txt");
            fail("Property xxx is not available");
        }
        catch (Exception e)
        {
            //Expected
        }

        result = parser.getFilename(adapter, "Test9_[header:xxx?].txt");
        assertEquals("Test9_[header:xxx?].txt", result);
    }

}