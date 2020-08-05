/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling;

import static java.util.Arrays.asList;
import static org.mule.runtime.extension.api.values.ValueResolvingException.MISSING_REQUIRED_PARAMETERS;
import org.mule.runtime.app.declaration.api.ComponentElementDeclaration;

import java.util.List;

import org.junit.Test;

public class ComponentValueProviderTestCase extends DeclarationSessionTestCase {

  @Test
  public void configLessConnectionLessOnOperation() {
    ComponentElementDeclaration elementDeclaration = configLessConnectionLessOPDeclaration(CONFIG_NAME);
    validateSuccess(session, elementDeclaration, PROVIDED_PARAMETER_NAME, "ConfigLessConnectionLessNoActingParameter");
  }

  @Test
  public void configLessConnectionLessOnOperationWithMissingConfigWorks() {
    ComponentElementDeclaration elementDeclaration = configLessConnectionLessOPDeclaration("");
    validateSuccess(session, elementDeclaration, PROVIDED_PARAMETER_NAME, "ConfigLessConnectionLessNoActingParameter");
  }

  @Test
  public void configLessOnOperation() {
    ComponentElementDeclaration elementDeclaration = configLessOPDeclaration(CONFIG_NAME);
    validateSuccess(session, elementDeclaration, PROVIDED_PARAMETER_NAME, CLIENT_NAME);
  }

  @Test
  public void configLessOnOperationFailsWithMissingConfig() {
    ComponentElementDeclaration elementDeclaration = configLessOPDeclaration("");
    validateFailure(session,
                    elementDeclaration,
                    PROVIDED_PARAMETER_NAME,
                    "The value provider requires a connection and none was provided",
                    MISSING_REQUIRED_PARAMETERS);
  }

  @Test
  public void actingParameterOnOperation() {
    final String actingParameter = "actingParameter";
    ComponentElementDeclaration elementDeclaration = actingParameterOPDeclaration(CONFIG_NAME, actingParameter);
    validateSuccess(session, elementDeclaration, PROVIDED_PARAMETER_NAME, WITH_ACTING_PARAMETER + actingParameter);
  }

  @Test
  public void missingActingParameterFails() {
    final String actingParameter = "actingParameter";
    ComponentElementDeclaration elementDeclaration = actingParameterOPDeclaration(CONFIG_NAME, actingParameter);
    elementDeclaration.getParameterGroups().get(0).getParameters().remove(0);
    validateFailure(session, elementDeclaration, PROVIDED_PARAMETER_NAME,
                    "Unable to retrieve values. There are missing required parameters for the resolution: [actingParameter]",
                    MISSING_REQUIRED_PARAMETERS);
  }

  @Test
  public void actingParameterGroup() {
    final String stringValue = "stringValue";
    final int intValue = 0;
    final List<String> listValue = asList("one", "two", "three");
    ComponentElementDeclaration elementDeclaration =
        actingParameterGroupOPDeclaration(CONFIG_NAME, stringValue, intValue, listValue);
    validateSuccess(session, elementDeclaration, PROVIDED_PARAMETER_NAME, "stringValue-0-one-two-three");
  }

  @Test
  public void missingParameterFromParameterGroupFails() {
    final String stringValue = "stringValue";
    final int intValue = 0;
    final List<String> listValue = asList("one", "two", "three");

    ComponentElementDeclaration elementDeclaration =
            actingParameterGroupOPDeclaration(CONFIG_NAME, stringValue, intValue, listValue);
    elementDeclaration.getParameterGroups().get(0).getParameters().remove(0);
    validateFailure(session, elementDeclaration, PROVIDED_PARAMETER_NAME, "Unable to retrieve values. There are missing required parameters for the resolution: [stringParam]", MISSING_REQUIRED_PARAMETERS);

    elementDeclaration =
            actingParameterGroupOPDeclaration(CONFIG_NAME, stringValue, intValue, listValue);
    elementDeclaration.getParameterGroups().get(0).getParameters().remove(1);
    validateFailure(session, elementDeclaration, PROVIDED_PARAMETER_NAME, "Unable to retrieve values. There are missing required parameters for the resolution: [intParam]", MISSING_REQUIRED_PARAMETERS);

    elementDeclaration =
            actingParameterGroupOPDeclaration(CONFIG_NAME, stringValue, intValue, listValue);
    elementDeclaration.getParameterGroups().get(0).getParameters().remove(2);
    validateFailure(session, elementDeclaration, PROVIDED_PARAMETER_NAME, "Unable to retrieve values. There are missing required parameters for the resolution: [listParams]", MISSING_REQUIRED_PARAMETERS);

    elementDeclaration =
            actingParameterGroupOPDeclaration(CONFIG_NAME, stringValue, intValue, listValue);
    elementDeclaration.getParameterGroups().get(0).getParameters().remove(0);
    elementDeclaration.getParameterGroups().get(0).getParameters().remove(0);
    elementDeclaration.getParameterGroups().get(0).getParameters().remove(0);
    validateFailure(session, elementDeclaration, PROVIDED_PARAMETER_NAME, "Unable to retrieve values. There are missing required parameters for the resolution: [stringParam, intParam, listParams]", MISSING_REQUIRED_PARAMETERS);
  }

  @Test
  public void complexActingParameter() {
    final String stringValue = "stringValue";
    ComponentElementDeclaration elementDeclaration =
        complexActingParameterOPDeclaration(CONFIG_NAME, stringValue);
    validateSuccess(session, elementDeclaration, PROVIDED_PARAMETER_NAME, stringValue);
  }

}
