/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.functional.matcher;

import org.mule.extension.http.api.HttpResponseAttributes;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class HttpResponseAttributesStatusCodeMatcher extends TypeSafeMatcher<HttpResponseAttributes>
{
    private int statusCode;

    public HttpResponseAttributesStatusCodeMatcher(int statusCode)
    {
        this.statusCode = statusCode;
    }

    @Override
    protected boolean matchesSafely(HttpResponseAttributes item)
    {
        return item.getStatusCode() == statusCode;
    }

    @Override
    public void describeTo(Description description)
    {
        description.appendText("response attributes with status code ").appendValue(statusCode);
    }

    @Override
    protected void describeMismatchSafely(HttpResponseAttributes attributes, Description mismatchDescription)
    {
        mismatchDescription.appendText("got response attributes with status code ").appendValue(attributes.getStatusCode());
    }
}
