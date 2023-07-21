/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
      description.appendText("\tMessage: " + metadataFailure.getMessage() + lineSeparator());
      description.appendText("\t Reason: " + metadataFailure.getReason() + lineSeparator());
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
