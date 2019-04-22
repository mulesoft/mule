/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

@SmallTest
public class StringMessageUtilsTestCase extends AbstractMuleContextTestCase
{
    @Test
    public void toStringOnStringShouldReturnTheString()
    {
        String input = "Oscar";
        String result = StringMessageUtils.toString(input);
        assertEquals(input, result);
    }

    @Test
    public void toStringOnClassShouldReturnClassName()
    {
        Object test = getClass();
        String result = StringMessageUtils.toString(test);
        assertEquals(getClass().getName(), result);
    }

    @Test
    public void toStringOnObjectShouldReturnObjectsToStringRepresentation()
    {
        // this class uses the default toString implementation
        Object test = new TestObject("Ernie");
        String result = StringMessageUtils.toString(test);
        assertEquals(test.toString(), result);

        // this class has a custom toString implementation
        test = new AnotherTestObject("Bert");
        result = StringMessageUtils.toString(test);
        assertEquals("Bert", result);
    }

    @Test
    public void toStringOnStringArrayShouldReturnStringRepresentation()
    {
        Object test = new String[]{"foo", "bar"};
        String result = StringMessageUtils.toString(test);
        assertEquals("{foo,bar}", result);
    }

    @Test
    public void toStringOnByteArrayShouldReturnStringRepresentation()
    {
        Object test = new byte[]{1, 2};
        String result = StringMessageUtils.toString(test);
        assertEquals("{1,2}", result);
    }

    @Test
    public void toStringOnByteArrayLargerThanMaximumOutputLengthShouldReturnAbbreviatedStringRepresentation()
    {
        // create an array that is too long to be printed
        byte[] test = new byte[StringMessageUtils.MAX_ELEMENTS + 100];
        for (int i = 0; i < test.length; i++)
        {
            test[i] = (byte) i;
        }

        // the String will contain not more than exactly MAX_ARRAY_LENGTH elements
        String result = StringMessageUtils.toString(test);
        assertTrue(result.endsWith("[..]}"));
        assertEquals(StringMessageUtils.MAX_ELEMENTS - 1, StringUtils.countMatches(result, ","));
    }

    @Test
    public void toStringOnLongArrayShouldReturnStringRepresentation()
    {
        long[] test = new long[] { 5068875495743534L, 457635546759674L };
        Object result = StringMessageUtils.toString(test);
        assertEquals("{5068875495743534,457635546759674}", result);
    }

    @Test
    public void toStringOnDoubleArrayShouldReturnStringRepresentation()
    {
        double[] test = new double[] { 1.1, 2.02 };
        String result = StringMessageUtils.toString(test);
        assertEquals("{1.1,2.02}", result);
    }

    @Test
    public void toStringOnListLargerThanMaximumOutputLengthShouldReturnAbbreviatedStringRepresentation()
    {
        // create a Collection that is too long to be printed
        List<Integer> list = new ArrayList<Integer>(100);
        for (int i = 0; i < 100; i++)
        {
            list.add(new Integer(i));
        }

        // the String will contain not more than exactly MAX_ARRAY_LENGTH elements
        String result = StringMessageUtils.toString(list);
        assertTrue(result.endsWith("[..]]"));
        assertEquals(StringMessageUtils.MAX_ELEMENTS - 1, StringUtils.countMatches(result, ","));
    }

    @Test
    public void testFormattedString()
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

    @Test
    public void testBoilerPlateSingleLine()
    {
        String plate = StringMessageUtils.getBoilerPlate("Single message.", '*', 12);
        assertEquals(SystemUtils.LINE_SEPARATOR + "************" + SystemUtils.LINE_SEPARATOR
                     + "* Single   *" + SystemUtils.LINE_SEPARATOR + "* message. *"
                     + SystemUtils.LINE_SEPARATOR + "************", plate);
    }

    @Test
    public void testBoilerPlate()
    {
        List<String> msgs = new ArrayList<>();
        msgs.add("This");
        msgs.add("is a");
        msgs.add("Boiler Plate");

        String plate = StringMessageUtils.getBoilerPlate(msgs, '*', 12);
        assertEquals(SystemUtils.LINE_SEPARATOR + "************" + SystemUtils.LINE_SEPARATOR
                     + "* This     *" + SystemUtils.LINE_SEPARATOR + "* is a     *"
                     + SystemUtils.LINE_SEPARATOR + "* Boiler   *" + SystemUtils.LINE_SEPARATOR
                     + "* Plate    *" + SystemUtils.LINE_SEPARATOR + "************", plate);

    }

    @Test
    public void testBoilerPlate2()
    {
        List<String> msgs = new ArrayList<>();
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

    @Test
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

    @Test
    public void headersToStringWithoutKeyWords()
    {
        MuleMessage message = getTestMuleMessage();
        message.setProperty("password", "123", PropertyScope.INBOUND);
        message.setProperty("secret", "shh", PropertyScope.INBOUND);
        message.setProperty("authorization", "granted", PropertyScope.INBOUND);

        String headers = StringMessageUtils.headersToString(message);

        assertThat(headers, not(containsString("123")));
        assertThat(headers, not(containsString("shh")));
        assertThat(headers, not(containsString("granted")));
    }

    private class TestObject
    {
        private String myName;

        public TestObject(String name)
        {
            this.myName = name;
        }

        public String getName()
        {
            return myName;
        }
    }

    private class AnotherTestObject extends TestObject
    {
        public AnotherTestObject(String name)
        {
            super(name);
        }

        @Override
        public String toString()
        {
            return getName();
        }
    }
}
