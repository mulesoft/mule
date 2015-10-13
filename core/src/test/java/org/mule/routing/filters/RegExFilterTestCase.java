/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.filters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;

public class RegExFilterTestCase extends AbstractMuleContextTestCase
{

    private static final String PATTERN = "(.*) brown fox";
    private RegExFilter regExWithValue;

    @Before
    public void setUp() throws Exception
    {
        regExWithValue = new RegExFilter("(\\w)* with the mules");
        regExWithValue.setMuleContext(muleContext);
    }


    @Test
    public void testRegexFilterNoPattern()
    {
        // start with default
        RegExFilter filter = new RegExFilter();
        assertNull(filter.getPattern());
        assertThat(filter.accept("No tengo dinero"), is(false));

        // activate a pattern
        filter.setPattern(PATTERN);
        assertThat(filter.accept("The quick brown fox"), is(true));

        // remove pattern again, i.e. block all
        filter.setPattern(null);
        assertThat(filter.accept("oh-oh"), is(false));
    }

    @Test
    public void testRegexFilter()
    {
        RegExFilter filter = new RegExFilter("The quick (.*)");
        assertNotNull(filter.getPattern());

        assertTrue(filter.accept("The quick brown fox"));
        assertTrue(filter.accept("The quick "));

        assertFalse(filter.accept("The quickbrown fox"));
        assertFalse(filter.accept("he quick brown fox"));

        filter.setPattern("(.*) brown fox");
        assertTrue(filter.accept("The quick brown fox"));
        assertTrue(filter.accept(" brown fox"));

        assertFalse(filter.accept("The quickbrown fox"));
        assertFalse(filter.accept("The quick brown fo"));

        filter.setPattern("(.*) brown (.*)");
        assertTrue(filter.accept("The quick brown fox"));
        assertTrue(filter.accept("(.*) brown fox"));

        assertFalse(filter.accept("The quickbrown fox"));
        assertTrue(filter.accept("The quick brown fo"));

        filter.setPattern("(.*)");
        assertTrue(filter.accept("The quick brown fox"));
    }

    @Test
    public void testNullInput()
    {
        RegExFilter filter = new RegExFilter("The quick (.*)");
        assertNotNull(filter.getPattern());
        assertFalse(filter.accept((Object) null));
    }

    @Test
    public void testMuleMessageInput()
    {
        RegExFilter filter = new RegExFilter("The quick (.*)");
        assertNotNull(filter.getPattern());
        MuleMessage message = new DefaultMuleMessage("The quick brown fox", muleContext);
        assertTrue(filter.accept(message));
    }

    @Test
    public void testByteArrayInput()
    {
        System.setProperty(MuleProperties.MULE_ENCODING_SYSTEM_PROPERTY, "UTF-8");
        RegExFilter filter = new RegExFilter("The quick (.*)");
        assertNotNull(filter.getPattern());

        byte[] bytes = "The quick brown fox".getBytes();
        assertTrue(filter.accept(bytes));
    }

    @Test
    public void testCharArrayInput()
    {
        RegExFilter filter = new RegExFilter("The quick (.*)");
        assertNotNull(filter.getPattern());

        char[] chars = "The quick brown fox".toCharArray();
        assertTrue(filter.accept(chars));
    }

    @Test
    public void testEqualsWithSamePattern()
    {
        RegExFilter filter1 = new RegExFilter(PATTERN);
        RegExFilter filter2 = new RegExFilter(PATTERN);
        assertEquals(filter1, filter2);
    }

    @Test
    public void testEqualsWithDifferentPattern()
    {
        RegExFilter filter1 = new RegExFilter("foo");
        RegExFilter filter2 = new RegExFilter("bar");
        assertFalse(filter1.equals(filter2));
    }

    @Test
    public void testEqualsWithEqualPatternAndDifferentFlags()
    {
        RegExFilter filter1 = new RegExFilter(PATTERN, Pattern.DOTALL);
        RegExFilter filter2 = new RegExFilter(PATTERN, Pattern.CASE_INSENSITIVE);
        assertFalse(filter1.equals(filter2));

        filter1 = new RegExFilter(PATTERN, Pattern.DOTALL);
        filter2 = new RegExFilter(PATTERN, Pattern.DOTALL);
        assertEquals(filter1, filter2);
    }

    @Test
    public void equalsWithNullValues()
    {
        RegExFilter filter = new RegExFilter();
        filter.setPattern("");
        RegExFilter filter2 = new RegExFilter();
        filter2.setPattern("");
        assertThat(filter.equals(filter2), is(true));
    }

    @Test
    public void notEqualsWithDifferentValues()
    {
        RegExFilter filter = new RegExFilter();
        filter.setPattern("");
        filter.setValue("value");
        RegExFilter filter2 = new RegExFilter();
        filter2.setPattern("");
        filter2.setPattern("value2");
        assertThat(filter.equals(filter2), is(false));
    }

    @Test
    public void matchesValueFromMelPayload() throws InitialisationException
    {
        regExWithValue.setValue("#[payload]");
        regExWithValue.initialise();
        DefaultMuleMessage muleMessage = new DefaultMuleMessage("run with the mules", muleContext);
        assertThat(regExWithValue.accept(muleMessage), is(true));
    }

    @Test
    public void notMatchesValueFromMelPayload() throws InitialisationException
    {
        regExWithValue.setValue("#[payload]");
        regExWithValue.initialise();
        DefaultMuleMessage muleMessage = new DefaultMuleMessage("run with the zebras", muleContext);
        assertThat(regExWithValue.accept(muleMessage), is(false));
    }

    @Test
    public void matchesValueFromFlowVar() throws InitialisationException
    {
        regExWithValue.setValue("#[flowVars.value]");
        regExWithValue.initialise();
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(null, muleContext);
        muleMessage.setInvocationProperty("value", "code with the mules");
        assertThat(regExWithValue.accept(muleMessage), is(true));
    }

    @Test
    public void matchesPlainTextValue() throws InitialisationException
    {
        regExWithValue.setValue("run with the mules");
        regExWithValue.initialise();
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(null, muleContext);
        assertThat(regExWithValue.accept(muleMessage), is(true));
    }

}
