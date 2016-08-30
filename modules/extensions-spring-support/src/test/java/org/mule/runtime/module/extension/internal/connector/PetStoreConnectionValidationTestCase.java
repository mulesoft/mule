/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.connector;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import org.mule.runtime.core.exception.MessagingException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class PetStoreConnectionValidationTestCase extends PetStoreConnectionTestCase {

  private static final String INVALID_CREDENTIALS_ERROR_MESSAGE = "Invalid credentials.";


  @Rule
  public ExpectedException expectedEx = ExpectedException.none();

  @Override
  protected String getConfigFile() {
    return "petstore-simple-connection.xml";
  }

  @Test
  public void getInvalidConnection() throws Exception {
    expectedEx.expect(MessagingException.class);
    expectedEx.expectCause(is(instanceOf(Exception.class)));
    expectedEx.expectMessage(is(INVALID_CREDENTIALS_ERROR_MESSAGE));

    runFlow("getPetsWithInvalidConfigWithConnectionValidation").getMessage().getPayload();
  }

  @Test
  public void getInvalidConnectionWithDisabledValidation() throws Exception {
    runFlow("getPetsWithInvalidConfigAndDisabledValidation").getMessage().getPayload();
  }
}
