/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.matchers;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class MuleMessagetInboundAttachmentInputStreamAsStringMatcher extends TypeSafeMatcher<InputStream>
{

    private Matcher<String> matcher;
    private String inboundAttachmentValue = null;

    public MuleMessagetInboundAttachmentInputStreamAsStringMatcher(Matcher<String> matcherGiven)
    {
        matcher = matcherGiven;
    }

    @Override
    public boolean matchesSafely(InputStream inputStream)
    {

        try
        {
            inboundAttachmentValue = IOUtils.toString(inputStream);
            return matcher.matches(inboundAttachmentValue);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void describeTo(Description description)
    {
        description.appendText("as string ")
                .appendDescriptionOf(matcher);
    }

    @Override
    protected void describeMismatchSafely(InputStream inputStreamMatcher, Description mismatchDescription)
    {
        if (inboundAttachmentValue != null)
        {
            matcher.describeMismatch(inboundAttachmentValue, mismatchDescription);
        }
        else
        {
            mismatchDescription.appendText("there was a problem transforming the content to string ");
        }
    }

    @Factory
    public static Matcher<InputStream> asString(Matcher<String> matcher)
    {
        return new MuleMessagetInboundAttachmentInputStreamAsStringMatcher(matcher);
    }
}
