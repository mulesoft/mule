/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.matchers;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class HttpResponseHeaderMatcher extends TypeSafeMatcher<HttpResponse>
{

    private String headerName;
    private Matcher<String> matcher;

    public HttpResponseHeaderMatcher(String headerNameGiven, Matcher<String> matcherGiven)
    {
        headerName = headerNameGiven;
        matcher = matcherGiven;
    }

    @Override
    public boolean matchesSafely(HttpResponse response)
    {
        Header header = response.getFirstHeader(headerName);
        return header != null && matcher.matches(header.getValue());
    }

    @Override
    public void describeTo(Description description)
    {
        description.appendText("a response that has the header <" + headerName + "> that ").appendDescriptionOf(matcher);
    }

    @Override
    protected void describeMismatchSafely(HttpResponse response, Description mismatchDescription)
    {
        mismatchDescription.appendText("was ").appendValue(response.getFirstHeader(headerName));
    }

    @Factory
    public static Matcher<HttpResponse> header(String headerName, Matcher<String> matcher)
    {
        return new HttpResponseHeaderMatcher(headerName, matcher);

    }
}

