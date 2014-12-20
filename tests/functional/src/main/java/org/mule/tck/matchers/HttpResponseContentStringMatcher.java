/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.matchers;

import org.mule.util.IOUtils;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class HttpResponseContentStringMatcher extends TypeSafeMatcher<HttpResponse>
{

    private Matcher<String> matcher;
    private String responseContent = null;

    public HttpResponseContentStringMatcher(Matcher<String> matcherToUse)
    {
        matcher = matcherToUse;
    }

    @Override
    public boolean matchesSafely(HttpResponse response)
    {
        try
        {
            responseContent = IOUtils.toString(response.getEntity().getContent());
        }
        catch (IOException e)
        {
            responseContent = null;
        }

        return matcher.matches(responseContent);
    }

    @Override
    public void describeTo(Description description)
    {
        description.appendText("a response that the content value ").appendDescriptionOf(matcher);
    }

    @Override
    protected void describeMismatchSafely(HttpResponse response, Description mismatchDescription)
    {

        mismatchDescription.appendText("was ").appendValue(responseContent);
    }

    @Factory
    public static Matcher<HttpResponse> contentValue(Matcher<String> matcher)
    {
        return new HttpResponseContentStringMatcher(matcher);

    }
}

