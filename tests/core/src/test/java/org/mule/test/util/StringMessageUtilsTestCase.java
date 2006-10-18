/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.lang.SystemUtils;
import org.mule.util.StringMessageUtils;
import org.mule.util.StringUtils;

public class StringMessageUtilsTestCase extends TestCase
{

    public void testToString() throws Exception
    {
        Object test = "Oscar";
        Object result = StringMessageUtils.toString(test);
        assertEquals("Oscar", result);

        test = getClass();
        result = StringMessageUtils.toString(test);
        assertEquals(getClass().getName(), result);

        test = new TestObject("Ernie");
        result = StringMessageUtils.toString(test);
        assertEquals(test.toString(), result);

        test = new AnotherTestObject("Bert");
        result = StringMessageUtils.toString(test);
        assertEquals("Bert", result);

        test = new Object[]{"foo", "bar"};
        result = StringMessageUtils.toString(test);
        assertEquals("{foo,bar}", result);

        test = new byte[]{1, 2};
        result = StringMessageUtils.toString(test);
        assertEquals("{1,2}", result);

        // create an array that is too long to be printed
        test = new byte[StringMessageUtils.MAX_ELEMENTS + 100];
        for (int i = 0; i < ((byte[])test).length; i++)
        {
            ((byte[])test)[i] = (byte)i;
        }

        // the String will contain not more than exactly MAX_ARRAY_LENGTH elements
        result = StringMessageUtils.toString(test);
        assertTrue(((String)result).endsWith("[..]}"));
        assertEquals(StringMessageUtils.MAX_ELEMENTS - 1, StringUtils.countMatches((String)result, ","));

        test = new long[]{5068875495743534L, 457635546759674L};
        result = StringMessageUtils.toString(test);
        assertEquals("{5068875495743534,457635546759674}", result);

        test = new double[]{1.1, 2.02};
        result = StringMessageUtils.toString(test);
        assertEquals("{1.1,2.02}", result);

        // create a Collection that is too long to be printed
        test = new ArrayList(100);
        for (int i = 0; i < 100; i++)
        {
            ((Collection)test).add(new Integer(i));
        }

        // the String will contain not more than exactly MAX_ARRAY_LENGTH elements
        result = StringMessageUtils.toString(test);
        assertTrue(((String)result).endsWith("[..]]"));
        assertEquals(StringMessageUtils.MAX_ELEMENTS - 1, StringUtils.countMatches((String)result, ","));

    }

    public void testFormattedString() throws Exception
    {
        String result;
        String msg1 = "There is not substitution here";

        result = StringMessageUtils.getFormattedMessage(msg1, null);
        assertEquals(msg1, result);

        result = StringMessageUtils.getFormattedMessage(msg1, new Object[]{});
        assertEquals(msg1, result);

        String msg2 = "There should be a variable {0}, {1} and {2}";
        result = StringMessageUtils.getFormattedMessage(msg2, new Object[]{"here", "there", "everywhere"});
        assertEquals("There should be a variable here, there and everywhere", result);
    }

    public void testBoilerPlateSingleLine()
    {
        String plate = StringMessageUtils.getBoilerPlate("Single message.", '*', 12);
        assertEquals(SystemUtils.LINE_SEPARATOR + "************" + SystemUtils.LINE_SEPARATOR
                     + "* Single   *" + SystemUtils.LINE_SEPARATOR + "* message. *"
                     + SystemUtils.LINE_SEPARATOR + "************", plate);
    }

    public void testBoilerPlate() throws Exception
    {
        List msgs = new ArrayList();
        msgs.add("This");
        msgs.add("is a");
        msgs.add("Boiler Plate");

        String plate = StringMessageUtils.getBoilerPlate(msgs, '*', 12);
        assertEquals(SystemUtils.LINE_SEPARATOR + "************" + SystemUtils.LINE_SEPARATOR
                     + "* This     *" + SystemUtils.LINE_SEPARATOR + "* is a     *"
                     + SystemUtils.LINE_SEPARATOR + "* Boiler   *" + SystemUtils.LINE_SEPARATOR
                     + "* Plate    *" + SystemUtils.LINE_SEPARATOR + "************", plate);

    }

    public void testBoilerPlate2() throws Exception
    {
        List msgs = new ArrayList();
        msgs.add("This");
        msgs.add("is a");
        msgs.add("Boiler Plate Message that should get wrapped to the next line if it is working properly");

        String plate = StringMessageUtils.getBoilerPlate(msgs, '*', 12);
        assertEquals(SystemUtils.LINE_SEPARATOR + "************" + SystemUtils.LINE_SEPARATOR
                     + "* This     *" + SystemUtils.LINE_SEPARATOR + "* is a     *"
                     + SystemUtils.LINE_SEPARATOR + "* Boiler   *" + SystemUtils.LINE_SEPARATOR
                     + "* Plate    *" + SystemUtils.LINE_SEPARATOR + "* Message  *"
                     + SystemUtils.LINE_SEPARATOR + "* that     *" + SystemUtils.LINE_SEPARATOR
                     + "* should   *" + SystemUtils.LINE_SEPARATOR + "* get      *"
                     + SystemUtils.LINE_SEPARATOR + "* wrapped  *" + SystemUtils.LINE_SEPARATOR
                     + "* to the   *" + SystemUtils.LINE_SEPARATOR + "* next     *"
                     + SystemUtils.LINE_SEPARATOR + "* line if  *" + SystemUtils.LINE_SEPARATOR
                     + "* it is    *" + SystemUtils.LINE_SEPARATOR + "* working  *"
                     + SystemUtils.LINE_SEPARATOR + "* properly *" + SystemUtils.LINE_SEPARATOR
                     + "************", plate);
    }

    public void testTruncate()
    {
        String msg = "this is a test message for truncating";
        String result = StringMessageUtils.truncate(msg, 100, true);
        assertEquals(msg, result);

        result = StringMessageUtils.truncate(msg, 10, false);
        assertEquals("this is a ...", result);

        result = StringMessageUtils.truncate(msg, 10, true);
        assertEquals("this is a ...[10 of 37]", result);
    }

    private class TestObject
    {
        private String name;

        public TestObject(String name)
        {
            this.name = name;
        }

        public String getName()
        {
            return name;
        }
    }

    private class AnotherTestObject extends TestObject
    {
        public AnotherTestObject(String name)
        {
            super(name);
        }

        public String toString()
        {
            return getName();
        }
    }

}
