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

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.tck.AbstractMuleTestCase;

/**
 * Test the syntax of the SimpleFilename parser
 */
public class ExpressionFilenameParserTestCase extends AbstractMuleTestCase
{
    private ExpressionFilenameParser parser;
    private MuleMessage message;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        parser = new ExpressionFilenameParser();
        parser.setMuleContext(muleContext);

        message = new DefaultMuleMessage("hello", muleContext);
        message.setProperty("foo", "bar");
        message.setProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME, "originalName");
        message.setProperty(FileConnector.PROPERTY_FILENAME, "newName");
    }

    public void testWigglyMuleStyleParsing()
    {
        String result = parser.getFilename(message, "Test1_#[function:count].txt");
        assertEquals("Test1_0.txt", result);

        result = parser.getFilename(message, "Test2_#[function:datestamp-yyMMdd].txt");
        assertEquals(16, result.length());

        result = parser.getFilename(message, "Test3_#[function:datestamp].txt");
        assertEquals(31, result.length());

        result = parser.getFilename(message, "Test4_#[function:systime].txt");
        assertFalse(result.equals("Test4_#[function:systime].txt"));

        result = parser.getFilename(message, "Test5_#[function:uuid].txt");
        assertFalse(result.equals("Test5_#[function:uuid].txt"));

        result = parser.getFilename(message, "Test6_#[function:count].txt");
        assertEquals("Test6_1.txt", result);

        result = parser.getFilename(message, "Test7_#[header:originalFilename].txt");
        assertEquals("Test7_originalName.txt", result);

        result = parser.getFilename(message, "Test8_#[header:foo].txt");
        assertEquals("Test8_bar.txt", result);

        result = parser.getFilename(message, "Test9_#[header:xxx?].txt");
        assertEquals("Test9_#[header:xxx?].txt", result);

        try
        {
            result = parser.getFilename(message, "Test9_#[header:xxx].txt");
            fail("Property xxx is not available");
        }
        catch (Exception e)
        {
            //Expected
        }
    }

    public void testSquareStyleParsing()
    {
        String result = parser.getFilename(message, "Test1_[function:count].txt");
        assertEquals("Test1_0.txt", result);

        result = parser.getFilename(message, "Test2_[function:dateStamp-yyMMdd].txt");
        assertEquals("got result: " + result, 16, result.length());

        result = parser.getFilename(message, "Test3_[function:dateStamp].txt");
        assertEquals("got result: '" + result, 31, result.length());

        result = parser.getFilename(message, "Test4_[function:systime].txt");
        assertFalse(result.equals("Test4_[function:systime].txt"));

        result = parser.getFilename(message, "Test5_[function:uuid].txt");
        assertFalse(result.equals("Test5_[function:uuid].txt"));

        result = parser.getFilename(message, "Test6_[function:count].txt");
        assertEquals("Test6_1.txt", result);

        result = parser.getFilename(message, "Test7_[header:originalFilename].txt");
        assertEquals("Test7_originalName.txt", result);

        result = parser.getFilename(message, "Test8_[header:foo].txt");
        assertEquals("Test8_bar.txt", result);

        try
        {
            result = parser.getFilename(message, "Test9_[header:xxx].txt");
            fail("Property xxx is not available");
        }
        catch (Exception e)
        {
            //Expected
        }

        result = parser.getFilename(message, "Test9_[header:xxx?].txt");
        assertEquals("Test9_[header:xxx?].txt", result);
    }
}