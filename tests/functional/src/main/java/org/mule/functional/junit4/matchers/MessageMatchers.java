/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4.matchers;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;

import org.hamcrest.Factory;
import org.hamcrest.Matcher;

public class MessageMatchers {

  /**
   * Verifies the {@link Message}'s attributes using any {@link Message#getAttributes()} matcher.
   */
  @Factory
  public static <T> Matcher<Message> hasAttributes(Matcher<T> attributesMatcher) {
    return new IsMessageWithAttributes<>(attributesMatcher);
  }

  /**
   * Verifies the {@link Message}'s payload using a String matcher. Only works with String and InputStream payloads.
   */
  @Factory
  public static Matcher<Message> hasPayload(Matcher<String> matcher) {
    return new IsMessageWithPayload(matcher);
  }

  /**
   * Verifies the the {@link Message}'s media type via equals.
   */
  @Factory
  public static Matcher<Message> hasMediaType(MediaType mediaType) {
    return new IsMessageWithMediaType(mediaType);
  }
}
