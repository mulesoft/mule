/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.file;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Date;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
        message.setOutboundProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME, "originalName");
        message.setOutboundProperty(FileConnector.PROPERTY_FILENAME, "newName");
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

        result = parser.getFilename(message, "Test7_#[header:originalFilename].txt");
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
