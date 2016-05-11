/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.functional.matcher;

import org.mule.extension.http.api.HttpResponseAttributes;

import org.hamcrest.Factory;
import org.hamcrest.Matcher;

public class HttpMessageAttributesMatchers
{
    @Factory
    public static Matcher<HttpResponseAttributes> hasStatusCode(int statusCode)
    {
        return new HttpResponseAttributesStatusCodeMatcher(statusCode);
    }

    @Factory
    public static Matcher<HttpResponseAttributes> hasReasonPhrase(String reasonPhrase)
    {
        return new HttpResponseAttributesReasonPhraseMatcher(reasonPhrase);
    }
}
