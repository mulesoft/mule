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

public class MuleMessagePayloadObjectMatcher extends TypeSafeMatcher<MuleMessage>
{

    private Matcher<Object> matcher;
    private Object payload = null;

    public MuleMessagePayloadObjectMatcher(Matcher<Object> matcherGiven)
    {
        matcher = matcherGiven;
    }

    @Override
    public boolean matchesSafely(MuleMessage message)
    {
        try
        {
            payload = message.getPayload();
        }
        catch (Exception e)
        {
            payload = null;
        }

        return matcher.matches(payload);
    }

    @Override
    public void describeTo(Description description)
    {
        description.appendText("a payload that ").appendDescriptionOf(matcher);
    }

    @Override
    protected void describeMismatchSafely(MuleMessage message, Description mismatchDescription)
    {
        mismatchDescription.appendText("was ").appendValue(payload);
    }

    @Factory
    public static Matcher<MuleMessage> payload(Matcher<Object> matcher)
    {
        return new MuleMessagePayloadObjectMatcher(matcher);

    }
}

