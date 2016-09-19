/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4.matchers;

import org.mule.runtime.api.message.MultiPartPayload;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * Verifies a {@link MultiPartPayload}'s part count.
 *
 * @since 4.0
 */
public class IsMultiPartPayloadWithSize extends TypeSafeMatcher<MultiPartPayload> {

  private final int expectedSize;

  public IsMultiPartPayloadWithSize(int size) {
    this.expectedSize = size;
  }

  @Override
  protected boolean matchesSafely(MultiPartPayload multiPartPayload) {
    return multiPartPayload.getParts().size() == expectedSize;
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("multipart payload with ").appendValue(expectedSize).appendText(" parts");
  }

  @Override
  protected void describeMismatchSafely(MultiPartPayload multiPartPayload, Description mismatchDescription) {
    mismatchDescription.appendText("got multipart payload with ");
    mismatchDescription.appendValue(multiPartPayload.getParts().size()).appendText(" parts");
  }
}
