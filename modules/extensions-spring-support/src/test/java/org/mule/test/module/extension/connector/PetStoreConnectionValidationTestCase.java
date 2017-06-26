/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.connector;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.rules.ExpectedException.none;

import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.test.module.extension.config.PetStoreConnectionTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class PetStoreConnectionValidationTestCase extends PetStoreConnectionTestCase {

  private static final String INVALID_CREDENTIALS_ERROR_MESSAGE = "Invalid credentials";


  @Rule
  public ExpectedException expectedEx = none();

  @Override
  protected String getConfigFile() {
    return "petstore-simple-connection.xml";
  }

  @Test
  public void getInvalidConnection() throws Exception {
    expectedEx.expect(MessagingException.class);
    expectedEx.expectCause(is(instanceOf(Exception.class)));
    expectedEx.expectMessage(containsString(INVALID_CREDENTIALS_ERROR_MESSAGE));

    runFlow("getPetsWithInvalidConfigWithConnectionValidation").getMessage().getPayload().getValue();
  }

  @Test
  public void getInvalidConnectionWithDisabledValidation() throws Exception {
    runFlow("getPetsWithInvalidConfigAndDisabledValidation").getMessage().getPayload().getValue();
  }
}
