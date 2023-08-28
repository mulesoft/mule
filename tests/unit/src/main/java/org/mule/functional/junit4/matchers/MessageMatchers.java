/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.functional.junit4.matchers;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;

import org.hamcrest.Matcher;

public class MessageMatchers {

  /**
   * Verifies the {@link Message}'s attributes using any {@link Message#getAttributes()} matcher.
   */
  public static <T> Matcher<Message> hasAttributes(Matcher<T> attributesMatcher) {
    return new IsMessageWithAttributes<>(attributesMatcher);
  }

  /**
   * Verifies the {@link Message}'s payload using a String matcher. Only works with String and InputStream payloads.
   */
  public static Matcher<Message> hasPayload(Matcher<String> matcher) {
    return new IsMessageWithPayload(matcher);
  }

  /**
   * Verifies the the {@link Message}'s media type via equals.
   */
  public static Matcher<Message> hasMediaType(MediaType mediaType) {
    return new IsMessageWithMediaType(mediaType);
  }
}
