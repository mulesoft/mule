/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.matchers;

import org.mule.api.MuleMessage;

import javax.activation.DataHandler;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class MuleMessagetInboundAttachmentMatcher extends TypeSafeMatcher<MuleMessage>
{

    private Matcher<DataHandler> matcher;
    private String inboundAttachmentName = null;
    private DataHandler inboundAttachmentValue = null;

    public MuleMessagetInboundAttachmentMatcher(String inboundPropertyNameGiven, Matcher<DataHandler> matcherGiven)
    {
        inboundAttachmentName = inboundPropertyNameGiven;
        matcher = matcherGiven;
    }

    @Override
    public boolean matchesSafely(MuleMessage message)
    {
        inboundAttachmentValue = message.getInboundAttachment(inboundAttachmentName);
        return inboundAttachmentValue != null && matcher.matches(inboundAttachmentValue);
    }

    @Override
    public void describeTo(Description description)
    {
        description.appendText("That the inbound attachment ")
                   .appendValue(inboundAttachmentName)
                   .appendText(" ")
                   .appendDescriptionOf(matcher);
    }

    @Override
    protected void describeMismatchSafely(MuleMessage message, Description mismatchDescription)
    {
        if(inboundAttachmentValue != null){
            matcher.describeMismatch(inboundAttachmentValue,mismatchDescription);
        }
        else
        {
            mismatchDescription.appendText("doesn't exist an inbound attachment with the name ").appendValue(inboundAttachmentName);
        }
    }

    @Factory
    public static Matcher<MuleMessage> inboundAttachment(String inboundPropertyName, Matcher<DataHandler> matcher)
    {
        return new MuleMessagetInboundAttachmentMatcher(inboundPropertyName, matcher);
    }
}

