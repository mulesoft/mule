/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.matcher;

import org.apache.http.HttpResponse;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class HttpResponseReasonPhraseMatcher extends TypeSafeMatcher<HttpResponse> {

  private String reasonPhrase;

  public HttpResponseReasonPhraseMatcher(String reasonPhrase) {
    this.reasonPhrase = reasonPhrase;
  }

  @Override
  public boolean matchesSafely(HttpResponse response) {
    return reasonPhrase.equals(response.getStatusLine().getReasonPhrase());
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("a response with reason phrase ").appendValue(reasonPhrase);
  }

  @Override
  protected void describeMismatchSafely(HttpResponse response, Description mismatchDescription) {
    mismatchDescription.appendText("got a response with reason phrase ").appendValue(response.getStatusLine().getReasonPhrase());
  }

  @Factory
  public static Matcher<HttpResponse> hasReasonPhrase(String reasonPhrease) {
    return new HttpResponseReasonPhraseMatcher(reasonPhrease);
  }
}
