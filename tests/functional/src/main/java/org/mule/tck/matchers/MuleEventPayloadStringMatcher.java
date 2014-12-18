/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.matchers;

import org.mule.api.MuleEvent;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class MuleEventPayloadStringMatcher extends TypeSafeMatcher<MuleEvent>
{

    private Matcher<String> matcher;
    private String payloadAsString = null;

    public MuleEventPayloadStringMatcher(Matcher<String> matcherGiven)
    {
        matcher = matcherGiven;
    }

    @Override
    public boolean matchesSafely(MuleEvent event)
    {
        try
        {
            payloadAsString = event.getMessage().getPayloadAsString();
        }
        catch (Exception e)
        {
            payloadAsString = null;
        }

        return matcher.matches(payloadAsString);
    }

    @Override
    public void describeTo(Description description)
    {
        description.appendText("a payload as string that ").appendDescriptionOf(matcher);
    }

    @Override
    protected void describeMismatchSafely(MuleEvent event, Description mismatchDescription)
    {
        mismatchDescription.appendText("was ").appendValue(payloadAsString);
    }

    @Factory
    public static Matcher<MuleEvent> payloadAsString(Matcher<String> matcher)
    {
        return new MuleEventPayloadStringMatcher(matcher);

    }
}

