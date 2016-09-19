/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4.matchers;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.message.MultiPartPayload;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Verifies whether a {@link MultiPartPayload} contains a part matching a given {@link Message} matcher.
 *
 * @since 4.0
 */
public class IsMultiPartPayloadContainingPart extends TypeSafeMatcher<MultiPartPayload> {

  private final Matcher<Message> matcher;

  public IsMultiPartPayloadContainingPart(Matcher<Message> matcher) {
    this.matcher = matcher;
  }

  @Override
  protected boolean matchesSafely(MultiPartPayload multiPartPayload) {
    return multiPartPayload.getParts().stream().anyMatch(matcher::matches);
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("multipart payload containing ");
    description.appendDescriptionOf(matcher);
  }

  @Override
  protected void describeMismatchSafely(MultiPartPayload multiPartPayload, Description mismatchDescription) {
    mismatchDescription.appendText("got a multipart payload containing ").appendValue(multiPartPayload.getParts());
  }
}
