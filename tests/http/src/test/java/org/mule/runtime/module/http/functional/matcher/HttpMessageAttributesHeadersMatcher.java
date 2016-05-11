/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.functional.matcher;

import org.mule.extension.http.api.HttpAttributes;

import java.util.Map;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class HttpMessageAttributesHeadersMatcher extends TypeSafeMatcher<HttpAttributes>
{
    private Map<String, Object> headers;

    public HttpMessageAttributesHeadersMatcher(Map<String, Object> headers)
    {
        this.headers = headers;
    }

    @Override
    protected boolean matchesSafely(HttpAttributes item)
    {
        return false;
    }

    @Override
    public void describeTo(Description description)
    {

    }
}
