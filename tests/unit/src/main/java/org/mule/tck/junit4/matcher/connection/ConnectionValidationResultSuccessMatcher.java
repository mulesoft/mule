/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4.matcher.connection;

import static java.lang.System.lineSeparator;

import org.mule.runtime.api.connection.ConnectionValidationResult;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class ConnectionValidationResultSuccessMatcher extends TypeSafeMatcher<ConnectionValidationResult> {

  private ConnectionValidationResult item;

  @Override
  public void describeTo(Description description) {
    description.appendText("ConnectionValidationResult (" + item.getErrorType() + ")" + lineSeparator());
    description.appendText("\t  Message: " + item.getMessage() + lineSeparator());
    description.appendText("\tException: " + item.getException() + lineSeparator());

    // print for test troubleshooting
    item.getException().printStackTrace();
  }

  @Override
  protected boolean matchesSafely(ConnectionValidationResult item) {
    this.item = item;
    return item.isValid();
  }

  public static ConnectionValidationResultSuccessMatcher isSuccess() {
    return new ConnectionValidationResultSuccessMatcher();
  }

}
