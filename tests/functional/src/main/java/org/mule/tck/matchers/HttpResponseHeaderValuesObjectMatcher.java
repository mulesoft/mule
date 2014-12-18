/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.matchers;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class HttpResponseHeaderValuesObjectMatcher extends TypeSafeMatcher<HttpResponse>
{

    private String headerName;
    private Matcher<Collection<?>> matcher;
    Collection<String> headerValuesInCollection;

    public HttpResponseHeaderValuesObjectMatcher(String headerNameGiven, Matcher<Collection<?>> matcherGiven)
    {
        headerName = headerNameGiven;
        matcher = matcherGiven;
    }

    @Override
    public boolean matchesSafely(HttpResponse response)
    {
        Header[] headerValuesInArray = response.getHeaders(headerName);

        headerValuesInCollection = CollectionUtils.collect(Arrays.asList(headerValuesInArray), new Transformer()
        {
            @Override
            public Object transform(Object input)
            {
                Header header = (Header) input;
                return header.getValue();
            }
        });

        return headerValuesInArray != null && matcher.matches(headerValuesInCollection);
    }

    @Override
    public void describeTo(Description description)
    {
        description.appendText("a response that has the header <" + headerName + "> that ").appendDescriptionOf(matcher);
    }

    @Override
    protected void describeMismatchSafely(HttpResponse response, Description mismatchDescription)
    {
        //mismatchDescription.appendText("was ").appendValue(response.getFirstHeader(headerName));
        matcher.describeMismatch(headerValuesInCollection,mismatchDescription);
    }

    @Factory
    //public static Matcher<HttpResponse> headers(String headerName, Matcher<Collection<Object>> matcher)
    public static Matcher<HttpResponse> header(String headerName, Matcher<Collection<?>> matcher)
    {
        return new HttpResponseHeaderValuesObjectMatcher(headerName, matcher);
    }
}

