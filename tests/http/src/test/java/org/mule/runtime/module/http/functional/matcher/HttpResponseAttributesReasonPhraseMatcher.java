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

public class HttpResponseAttributesReasonPhraseMatcher extends TypeSafeMatcher<HttpResponseAttributes>
{

    private final String reasonPhrase;

    public HttpResponseAttributesReasonPhraseMatcher(String reasonPhrase)
    {
        this.reasonPhrase = reasonPhrase;
    }

    @Override
    protected boolean matchesSafely(HttpResponseAttributes item)
    {
        return reasonPhrase.equals(item.getReasonPhrase());
    }

    @Override
    public void describeTo(Description description)
    {
        description.appendText("response attributes with reason phrase ").appendValue(reasonPhrase);
    }

    @Override
    protected void describeMismatchSafely(HttpResponseAttributes attributes, Description mismatchDescription)
    {
        mismatchDescription.appendText("got response attributes with reason phrase ").appendValue(attributes.getReasonPhrase());
    }
}
