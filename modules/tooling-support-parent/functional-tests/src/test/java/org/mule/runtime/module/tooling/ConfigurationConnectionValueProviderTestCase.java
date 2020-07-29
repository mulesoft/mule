/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling;

import static org.mule.runtime.module.tooling.ComponentValueProviderTestCase.getResultAndValidate;
import org.mule.runtime.app.declaration.api.ConfigurationElementDeclaration;
import org.mule.runtime.app.declaration.api.ConnectionElementDeclaration;
import org.mule.runtime.app.declaration.api.fluent.ArtifactDeclarer;

import org.junit.Test;

public class ConfigurationConnectionValueProviderTestCase extends DeclarationSessionTestCase {

  @Override
  protected void declareArtifact(ArtifactDeclarer artifactDeclarer) {}

  @Test
  public void testStaticValuesAtConnectionParameter() {
    ConnectionElementDeclaration connectionElementDeclaration = connectionDeclaration(CLIENT_NAME);
    getResultAndValidate(session, connectionElementDeclaration, PROVIDED_PARAMETER_NAME, "WITH-ACTING-PARAMETER-" + CLIENT_NAME);
  }

  @Test
  public void testStaticValuesAtConfigurationParameter() {
    ConfigurationElementDeclaration configurationElementDeclaration = configurationDeclaration(CLIENT_NAME);
    getResultAndValidate(session, configurationElementDeclaration, PROVIDED_PARAMETER_NAME,
                         "WITH-ACTING-PARAMETER-" + CLIENT_NAME);
  }

}
