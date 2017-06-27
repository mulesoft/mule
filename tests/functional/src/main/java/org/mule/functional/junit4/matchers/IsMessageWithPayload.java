/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4.matchers;

import static java.lang.String.format;
import static org.junit.Assert.fail;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.util.IOUtils;

import java.io.InputStream;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Verifies a {@link Message}'s payload, using a {@link String} matcher. Works with {@link String} and {@link InputStream}
 * payloads.
 *
 * @since 4.0
 */
public class IsMessageWithPayload<T> extends TypeSafeMatcher<Message> {

  private final Matcher<T> matcher;
  private Object incomingValue;

  public IsMessageWithPayload(Matcher<T> matcher) {
    this.matcher = matcher;
  }

  @Override
  protected boolean matchesSafely(Message message) {
    final Object payload = message.getPayload().getValue();
    if (payload instanceof String || payload instanceof CursorStreamProvider || payload instanceof InputStream) {
      // Save the String since we may consume the payload here
      incomingValue = getString(payload);
    } else {
      incomingValue = payload;
    }
    return matcher.matches(incomingValue);
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("message with payload ");
    description.appendDescriptionOf(matcher);
  }

  @Override
  protected void describeMismatchSafely(Message message, Description mismatchDescription) {
    mismatchDescription.appendText("got a message with a payload that ");
    matcher.describeMismatch(incomingValue, mismatchDescription);
  }

  private String getString(Object payload) {
    if (payload instanceof String) {
      return (String) payload;
    } else if (payload instanceof CursorStreamProvider) {
      return IOUtils.toString((CursorStreamProvider) payload);
    } else if (payload instanceof InputStream) {
      return IOUtils.toString((InputStream) payload);
    } else {
      fail(format("Expected String or InputStream payload but got [%s]",
                  payload != null ? payload.getClass().getName() : "null"));
      return null;
    }
  }
}
