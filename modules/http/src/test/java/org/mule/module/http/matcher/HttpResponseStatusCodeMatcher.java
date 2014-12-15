/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.matcher;

import org.apache.http.HttpResponse;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class HttpResponseStatusCodeMatcher extends TypeSafeMatcher<HttpResponse>
{

    private int statusCode;

    public HttpResponseStatusCodeMatcher(int statusCode)
    {
        this.statusCode = statusCode;
    }

    @Override
    public boolean matchesSafely(HttpResponse response)
    {
        return response.getStatusLine().getStatusCode() == statusCode;
    }

    @Override
    public void describeTo(Description description)
    {
        description.appendText("a response with status code ").appendValue(statusCode);
    }

    @Override
    protected void describeMismatchSafely(HttpResponse response, Description mismatchDescription)
    {
        mismatchDescription.appendText("got a response with status code ").appendValue(response.getStatusLine().getStatusCode());
    }

    @Factory
    public static Matcher<HttpResponse> hasStatusCode(int statusCode)
    {
        return new HttpResponseStatusCodeMatcher(statusCode);
    }
}