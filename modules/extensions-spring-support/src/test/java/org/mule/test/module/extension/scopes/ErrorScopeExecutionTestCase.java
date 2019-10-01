/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.scopes;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;

import org.mule.functional.api.exception.ExpectedError;
import org.mule.runtime.api.connection.ConnectionException;

import org.junit.Rule;
import org.junit.Test;

public class ErrorScopeExecutionTestCase extends AbstractScopeExecutionTestCase {

  @Rule
  public ExpectedError expectedError = ExpectedError.none();

  @Test
  public void failWithConnectivityErrorFromOtherExtension() throws Exception {
    expectedError.expectCause(instanceOf(ConnectionException.class));
    expectedError.expectErrorType("PETSTORE", "CONNECTIVITY");
    runFlow("failWithConnectivityErrorFromOtherExtension").getError();
  }

  @Test
  public void failWithCustomErrorFromOtherExtension() throws Exception {
    expectedError.expectErrorType("PETSTORE", "PET_ERROR");
    expectedError.expectMessage(containsString("Null content cannot be processed"));
    runFlow("failWithCustomErrorFromOtherExtension");
  }

}
