/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.http.impl.service.server.grizzly;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.service.http.api.HttpConstants.Method.GET;
import static org.mule.service.http.api.HttpConstants.Method.OPTIONS;
import static org.mule.service.http.api.HttpConstants.Method.PATCH;
import static org.mule.service.http.api.HttpConstants.Method.POST;
import org.mule.service.http.api.HttpConstants.Method;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.request.HttpRequestBuilder;
import org.mule.service.http.api.server.MethodRequestMatcher;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class DefaultMethodRequestMatcherTestCase extends AbstractMuleTestCase {

  private HttpRequestBuilder requestBuilder = HttpRequest.builder().setUri("uri");

  @Test(expected = IllegalArgumentException.class)
  public void doNotAcceptsEmptyString() {
    new DefaultMethodRequestMatcher(new String[] {});
  }

  @Test(expected = IllegalArgumentException.class)
  public void doNotAcceptsEmptyMethod() {
    new DefaultMethodRequestMatcher(new Method[] {});
  }

  @Test
  public void onlyAcceptsOneMethod() {

    final MethodRequestMatcher matcher =
        new DefaultMethodRequestMatcher(GET);
    assertThat(matcher.matches(requestBuilder.setMethod(GET).build()), is(true));
    assertThat(matcher.matches(requestBuilder.setMethod(POST).build()), is(false));
  }

  @Test
  public void acceptSeveralMethods() {
    final MethodRequestMatcher matcher = new DefaultMethodRequestMatcher(GET, POST, PATCH);
    assertThat(matcher.matches(requestBuilder.setMethod(GET).build()), is(true));
    assertThat(matcher.matches(requestBuilder.setMethod(POST).build()), is(true));
    assertThat(matcher.matches(requestBuilder.setMethod(PATCH).build()), is(true));
    assertThat(matcher.matches(requestBuilder.setMethod(OPTIONS).build()), is(false));
  }
}
