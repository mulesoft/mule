/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.matchers;

import javax.activation.DataHandler;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class MuleMessagetInboundAttachmentContentTypeMatcher extends TypeSafeMatcher<DataHandler>
{

    private Matcher<String> matcher;

    public MuleMessagetInboundAttachmentContentTypeMatcher(Matcher<String> matcherGiven)
    {
        matcher = matcherGiven;
    }

    @Override
    public boolean matchesSafely(DataHandler inboundAttachment)
    {
        return matcher.matches(inboundAttachment.getContentType());
    }

    @Override
    public void describeTo(Description description)
    {
        description.appendText("ContentType")
                   .appendText(" ")
                   .appendDescriptionOf(matcher);
    }

    @Override
    protected void describeMismatchSafely(DataHandler inboundAttachment, Description mismatchDescription)
    {
        mismatchDescription.appendText("was ").appendValue(inboundAttachment.getContentType());
    }

    @Factory
    public static Matcher<DataHandler> contentType(Matcher<String> matcher)
    {
        return new MuleMessagetInboundAttachmentContentTypeMatcher(matcher);
    }
}

