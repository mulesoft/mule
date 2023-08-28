/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.http.api.server;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.http.api.HttpConstants.Method.GET;
import static org.mule.runtime.http.api.HttpConstants.Method.OPTIONS;
import static org.mule.runtime.http.api.HttpConstants.Method.PATCH;
import static org.mule.runtime.http.api.HttpConstants.Method.POST;
import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_SERVICE;
import org.mule.runtime.http.api.HttpConstants.Method;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.request.HttpRequestBuilder;

import java.util.LinkedList;

import io.qameta.allure.Feature;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(HTTP_SERVICE)
public class MethodRequestMatcherTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private HttpRequestBuilder requestBuilder = HttpRequest.builder().uri("uri");

  @Test
  public void cannotBeEmpty() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("methods attribute should not be empty");
    MethodRequestMatcher.builder().build();
  }

  @Test
  public void cannotBeEmptyCollection() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("methods attribute should not be empty");
    MethodRequestMatcher.builder(new LinkedList<>()).build();
  }

  @Test
  public void cannotBeNullCollection() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("methods attribute should not be null");
    MethodRequestMatcher.builder(null).build();
  }

  @Test
  public void cannotBeNullString() {
    String none = null;
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("method attribute should not be null");
    MethodRequestMatcher.builder().add(none);
  }

  @Test
  public void cannotBeNullMethod() {
    Method none = null;
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("method attribute should not be null");
    MethodRequestMatcher.builder().add(none);
  }

  @Test
  public void onlyAcceptsOneMethod() {

    final MethodRequestMatcher matcher = MethodRequestMatcher.builder().add(GET).build();
    assertThat(matcher.matches(requestBuilder.method(GET).build()), is(true));
    assertThat(matcher.matches(requestBuilder.method(POST).build()), is(false));
  }

  @Test
  public void acceptSeveralMethods() {
    final MethodRequestMatcher matcher = MethodRequestMatcher.builder().add(GET).add(POST).add(PATCH).build();
    assertThat(matcher.matches(requestBuilder.method(GET).build()), is(true));
    assertThat(matcher.matches(requestBuilder.method(POST).build()), is(true));
    assertThat(matcher.matches(requestBuilder.method(PATCH).build()), is(true));
    assertThat(matcher.matches(requestBuilder.method(OPTIONS).build()), is(false));
  }
}
