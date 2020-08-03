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
import org.mule.runtime.app.declaration.api.fluent.ArtifactDeclarer;

import org.junit.Test;

public class ConnectivityTestingTestCase extends DeclarationSessionTestCase {

  private static String CONFIG_NAME_TEST_CONNECTION_FAILS = "configNameTestConnectionFails";

  @Override
  protected void declareArtifact(ArtifactDeclarer artifactDeclarer) {
    super.declareArtifact(artifactDeclarer);
    artifactDeclarer.withGlobalElement(configurationDeclaration(CONFIG_NAME_TEST_CONNECTION_FAILS,
                                                                connectionDeclaration("FAIL_TEST_CONNECTION")));
  }

  @Test
  public void testConnection() {
    ConnectionValidationResult connectionValidationResult = session.testConnection(CONFIG_NAME);
    assertThat(connectionValidationResult.isValid(), equalTo(true));
  }

  @Test
  public void testConnectionShouldNotDoneOnDeployment() {
    ConnectionValidationResult connectionValidationResult = session.testConnection(CONFIG_NAME_TEST_CONNECTION_FAILS);
    assertThat(connectionValidationResult.isValid(), equalTo(false));
    // Configuration has a connection provider instance that will fail validate connections and count the number of times
    // the validate method is called.
    assertThat(connectionValidationResult.getMessage(), equalTo("1"));
  }

  @Test
  public void testConnectionWrongConfigurationName() {
    ConnectionValidationResult connectionValidationResult = session.testConnection("invalidConfigName");
    assertThat(connectionValidationResult.isValid(), equalTo(false));
    assertThat(connectionValidationResult.getMessage(),
               equalTo("Could not find a connection provider for configuration: 'invalidConfigName'"));
  }

}
