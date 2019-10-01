/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4.matcher.metadata;

import static java.lang.System.lineSeparator;

import org.mule.runtime.api.metadata.resolving.MetadataFailure;
import org.mule.runtime.api.metadata.resolving.MetadataResult;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * Verifies that a {@link MetadataResult} is successful.
 *
 * @param <T> return type of the Metadata resolving operation.
 *
 * @since 4.3
 */
public class MetadataKeyResultSuccessMatcher<T> extends TypeSafeMatcher<MetadataResult<T>> {

  private MetadataResult<T> item;

  @Override
  public void describeTo(Description description) {
    for (MetadataFailure metadataFailure : item.getFailures()) {
      description.appendText("MetadataFailure (" + metadataFailure.getFailureCode() + " @ "
          + metadataFailure.getFailingComponent() + ") on " + metadataFailure.getFailingElement()
          + ":" + lineSeparator());
      description.appendText("\tMessaage: " + metadataFailure.getMessage() + lineSeparator());
      description.appendText("\t  Reason: " + metadataFailure.getReason() + lineSeparator());
    }
  }

  @Override
  protected boolean matchesSafely(MetadataResult<T> item) {
    this.item = item;
    return item.isSuccess() && item.getFailures().isEmpty();
  }

  public static <T> MetadataKeyResultSuccessMatcher<T> isSuccess() {
    return new MetadataKeyResultSuccessMatcher<>();
  }
}
