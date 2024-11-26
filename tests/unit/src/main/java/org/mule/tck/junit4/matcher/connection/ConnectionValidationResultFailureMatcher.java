/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4.matcher.connection;

import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.nullValue;

import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.message.ErrorType;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class ConnectionValidationResultFailureMatcher extends TypeSafeMatcher<ConnectionValidationResult> {

  private final Matcher<ErrorType> errorTypeMatcher;
  private final Matcher<String> messageMatcher;

  private ConnectionValidationResult item;

  public ConnectionValidationResultFailureMatcher(Matcher<ErrorType> errorTypeMatcher, Matcher<String> messageMatcher) {
    this.errorTypeMatcher = errorTypeMatcher;
    this.messageMatcher = messageMatcher;
  }

  @Override
  public void describeTo(Description description) {
    if (item.isValid()) {
      description.appendText("a `failure` ConnectionValidationResult");
    } else {
      description.appendText("a `failure` ConnectionValidationResult with error type ");
      errorTypeMatcher.describeTo(description);
      description.appendText("and a message ");
      messageMatcher.describeTo(description);
    }
  }

  @Override
  protected boolean matchesSafely(ConnectionValidationResult item) {
    this.item = item;
    return !item.isValid()
        && errorTypeMatcher.matches(item.getErrorType().orElse(null))
        && messageMatcher.matches(item.getMessage());
  }

  public static ConnectionValidationResultFailureMatcher isFailure() {
    return new ConnectionValidationResultFailureMatcher(anyOf(nullValue(), any(ErrorType.class)),
                                                        anyOf(nullValue(), any(String.class)));
  }

  public static ConnectionValidationResultFailureMatcher isFailure(Matcher<ErrorType> errorTypeMatcher,
                                                                   Matcher<String> messageMatcher) {
    return new ConnectionValidationResultFailureMatcher(errorTypeMatcher, messageMatcher);
  }

}
