/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener.matcher;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.module.http.internal.domain.request.HttpRequestBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class MethodRequestMatcherTestCase extends AbstractMuleTestCase
{

    @Test(expected =  IllegalArgumentException.class)
    public void doNotAcceptsNull()
    {
        new MethodRequestMatcher(null);
    }

    @Test
    public void onlyAcceptsOneMethod()
    {
        final MethodRequestMatcher matcher = new MethodRequestMatcher("GET");
        assertThat(matcher.matches(new HttpRequestBuilder().setMethod("GET").build()), is(true));
        assertThat(matcher.matches(new HttpRequestBuilder().setMethod("POST").build()), is(false));
    }

    @Test
    public void acceptSeveralMethods()
    {
        final MethodRequestMatcher matcher = new MethodRequestMatcher("GET", "POST", "PATCH");
        assertThat(matcher.matches(new HttpRequestBuilder().setMethod("GET").build()), is(true));
        assertThat(matcher.matches(new HttpRequestBuilder().setMethod("POST").build()), is(true));
        assertThat(matcher.matches(new HttpRequestBuilder().setMethod("PATCH").build()), is(true));
        assertThat(matcher.matches(new HttpRequestBuilder().setMethod("OPTIONS").build()), is(false));
    }
}
