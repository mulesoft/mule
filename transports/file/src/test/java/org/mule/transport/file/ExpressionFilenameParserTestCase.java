/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Date;

import org.junit.Test;

/**
 * Test the syntax of the SimpleFilename parser
 */
public class ExpressionFilenameParserTestCase extends AbstractMuleContextTestCase
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
        message.setOutboundProperty("foo", "bar");
        message.setProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME, "originalName", PropertyScope.INBOUND);
        message.setProperty(FileConnector.PROPERTY_FILENAME, "newName", PropertyScope.INBOUND);
    }

    @Test
    public void testWigglyMuleStyleParsing()
    {
        String result = parser.getFilename(message, "Test1_#[function:count].txt");
        assertEquals("Test1_0.txt", result);

        result = parser.getFilename(message, "Test2_#[function:datestamp-yyMMdd].txt");
        assertDatestampWithYearMonthAndDayMatches(result);
        
        result = parser.getFilename(message, "Test3_#[function:datestamp].txt");
        assertDefaultDatestampMatches(result);

        result = parser.getFilename(message, "Test4_#[function:systime].txt");
        assertFalse(result.equals("Test4_#[function:systime].txt"));

        result = parser.getFilename(message, "Test5_#[function:uuid].txt");
        assertFalse(result.equals("Test5_#[function:uuid].txt"));

        result = parser.getFilename(message, "Test6_#[function:count].txt");
        assertEquals("Test6_1.txt", result);

        result = parser.getFilename(message, "Test7_#[header:inbound:originalFilename].txt");
        assertEquals("Test7_originalName.txt", result);

        result = parser.getFilename(message, "Test8_#[header:foo].txt");
        assertEquals("Test8_bar.txt", result);

        result = parser.getFilename(message, "Test9_#[header:xxx?].txt");
        assertEquals("Test9_null.txt", result);

        try
        {
            result = parser.getFilename(message, "Test9_#[header:xxx].txt");
            fail("Property xxx is not available");
        }
        catch (Exception e)
        {
            // Expected
        }
    }

    @Test
    public void testSquareStyleParsing()
    {
        String result = parser.getFilename(message, "Test1_[function:count].txt");
        assertEquals("Test1_0.txt", result);

        result = parser.getFilename(message, "Test2_[function:dateStamp-yyMMdd].txt");
        assertDatestampWithYearMonthAndDayMatches(result);

        result = parser.getFilename(message, "Test3_[function:dateStamp].txt");
        assertDefaultDatestampMatches(result);

        result = parser.getFilename(message, "Test4_[function:systime].txt");
        assertFalse(result.equals("Test4_[function:systime].txt"));

        result = parser.getFilename(message, "Test5_[function:uuid].txt");
        assertFalse(result.equals("Test5_[function:uuid].txt"));

        result = parser.getFilename(message, "Test6_[function:count].txt");
        assertEquals("Test6_1.txt", result);

        result = parser.getFilename(message, "Test7_[header:inbound:originalFilename].txt");
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
            // Expected
        }

        result = parser.getFilename(message, "Test9_[header:xxx?].txt");
        assertEquals("Test9_null.txt", result);
    }

    private void assertDatestampWithYearMonthAndDayMatches(String result)
    {
        Date now = new Date();
        String expected = String.format("Test2_%1$ty%1$tm%1$td.txt", now);
        assertEquals(expected, result);
    }

    private void assertDefaultDatestampMatches(String result)
    {
        Date now = new Date();

        // can't compare exactly as the time differs between formatting the expected
        // result and the actual invocation of the function
        String expected = String.format("Test3_%1$td-%1$tm-%1$ty_%1$tH-%1$tM-.*.txt", now);

        assertTrue(result.matches(expected));
    }
}
