/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import org.mule.runtime.api.connection.ConnectionValidationResult;

import org.junit.Test;

public class ConnectivityTestingTestCase extends DeclarationSessionTestCase {

  @Test
  public void testConnection() {
    ConnectionValidationResult connectionValidationResult = session.testConnection(CONFIG_NAME);
    assertThat(connectionValidationResult.isValid(), equalTo(true));
  }

  @Test
  public void testConnectionWrongConfigurationName() {
    ConnectionValidationResult connectionValidationResult = session.testConnection("invalidConfigName");
    assertThat(connectionValidationResult.isValid(), equalTo(false));
    assertThat(connectionValidationResult.getMessage(),
               equalTo("Could not find a connection provider for configuration: 'invalidConfigName'"));
  }

}
