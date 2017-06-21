/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4.matchers;

import static org.mule.functional.junit4.matchers.MessageMatchers.hasAttributes;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.core.api.message.PartAttributes;

import org.hamcrest.Factory;
import org.hamcrest.Matcher;

public class MultiPartPayloadMatchers {

  /**
   * Verifies the amount of parts in a {@link MultiPartPayload}.
   */
  @Factory
  public static Matcher<MultiPartPayload> hasSize(int size) {
    return new IsMultiPartPayloadWithSize(size);
  }

  /**
   * Verifies whether a part that matches the specified {@link Message} matcher is contained in a {@link MultiPartPayload}.
   */
  @Factory
  public static Matcher<MultiPartPayload> hasPart(Matcher<Message> messageMatcher) {
    return new IsMultiPartPayloadContainingPart(messageMatcher);
  }

  /**
   * Verifies whether a part that matches the specified {@link PartAttributes} matcher is contained in a {@link MultiPartPayload}.
   */
  @Factory
  public static Matcher<MultiPartPayload> hasPartThat(Matcher<PartAttributes> partAttributesMatcher) {
    return new IsMultiPartPayloadContainingPart(hasAttributes(partAttributesMatcher));
  }

}
