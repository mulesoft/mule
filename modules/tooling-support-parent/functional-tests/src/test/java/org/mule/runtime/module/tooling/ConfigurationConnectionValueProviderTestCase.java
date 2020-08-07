/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling;

import static org.mule.runtime.extension.api.values.ValueResolvingException.MISSING_REQUIRED_PARAMETERS;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.ACTING_PARAMETER_NAME;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.configurationDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.connectionDeclaration;
import static org.mule.sdk.api.values.ValueResolvingException.INVALID_VALUE_RESOLVER_NAME;
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
    validateValuesSuccess(session, connectionElementDeclaration, PROVIDED_PARAMETER_NAME, "WITH-ACTING-PARAMETER-" + CLIENT_NAME);
  }

  @Test
  public void missingActingParameterAtConnectionFails() {
    ConnectionElementDeclaration connectionElementDeclaration = connectionDeclaration(CLIENT_NAME);
    connectionElementDeclaration.getParameterGroups().get(0).getParameters().remove(1);
    validateValuesFailure(session, connectionElementDeclaration, PROVIDED_PARAMETER_NAME,
                          "Unable to retrieve values. There are missing required parameters for the resolution: [actingParameter]",
                          MISSING_REQUIRED_PARAMETERS);
  }

  @Test
  public void testStaticValuesAtConfigurationParameter() {
    ConfigurationElementDeclaration configurationElementDeclaration = configurationDeclaration(CLIENT_NAME);
    validateValuesSuccess(session, configurationElementDeclaration, PROVIDED_PARAMETER_NAME,
                          "WITH-ACTING-PARAMETER-" + CLIENT_NAME);
  }

  @Test
  public void missingActingParameterAtConfigurationFails() {
    ConfigurationElementDeclaration configurationElementDeclaration = configurationDeclaration(CLIENT_NAME);
    configurationElementDeclaration.getParameterGroups().get(0).getParameters().remove(0);
    validateValuesFailure(session, configurationElementDeclaration, PROVIDED_PARAMETER_NAME,
                          "Unable to retrieve values. There are missing required parameters for the resolution: [actingParameter]",
                          MISSING_REQUIRED_PARAMETERS);
  }

  @Test
  public void getValuesOnParameterWithNoValueProviderOnConnection() {
    ConnectionElementDeclaration connectionElementDeclaration = connectionDeclaration(CLIENT_NAME);
    validateValuesFailure(session, connectionElementDeclaration, ACTING_PARAMETER_NAME,
                          "Unable to find model for parameter or parameter group with name 'actingParameter'.",
                          INVALID_VALUE_RESOLVER_NAME);
  }

  @Test
  public void getValuesOnParameterWithNoValueProviderOnConfig() {
    ConfigurationElementDeclaration configurationElementDeclaration = configurationDeclaration(CLIENT_NAME);
    validateValuesFailure(session, configurationElementDeclaration, ACTING_PARAMETER_NAME,
                          "Unable to find model for parameter or parameter group with name 'actingParameter'.",
                          INVALID_VALUE_RESOLVER_NAME);
  }

}
