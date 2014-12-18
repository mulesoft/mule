/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.matchers;

import org.mule.api.MuleMessage;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class MuleMessagetInboundPropertyStringMatcher extends TypeSafeMatcher<MuleMessage>
{

    private Matcher<String> matcher;
    private String inboundPropertyName = null;
    private Object inboundPropertyValue = null;

    public MuleMessagetInboundPropertyStringMatcher(String inboundPropertyNameGiven, Matcher<String> matcherGiven)
    {
        inboundPropertyName = inboundPropertyNameGiven;
        matcher = matcherGiven;
    }

    @Override
    public boolean matchesSafely(MuleMessage message)
    {
        inboundPropertyValue = message.getInboundProperty(inboundPropertyName);
        return inboundPropertyValue != null && matcher.matches(inboundPropertyValue.toString());
    }

    @Override
    public void describeTo(Description description)
    {
        description.appendText("That the value of the inbound property ")
                   .appendValue(inboundPropertyName)
                   .appendText(" ")
                   .appendDescriptionOf(matcher);
    }

    @Override
    protected void describeMismatchSafely(MuleMessage message, Description mismatchDescription)
    {
        mismatchDescription.appendText("was ");

        if(inboundPropertyValue != null){
            mismatchDescription.appendValue(inboundPropertyValue.toString());
        }
        else
        {
            mismatchDescription.appendValue(null);
        }
    }

    @Factory
    public static Matcher<MuleMessage> inboundProperty(String inboundPropertyName, Matcher<String> matcher)
    {
        return new MuleMessagetInboundPropertyStringMatcher(inboundPropertyName, matcher);
    }
}

