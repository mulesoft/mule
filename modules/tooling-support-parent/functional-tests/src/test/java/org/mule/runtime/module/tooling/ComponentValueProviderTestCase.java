/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import org.mule.runtime.api.value.ValueResult;
import org.mule.runtime.app.declaration.api.ComponentElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterizedElementDeclaration;
import org.mule.runtime.module.tooling.api.artifact.DeclarationSession;

import java.util.List;

import org.junit.Test;

public class ComponentValueProviderTestCase extends DeclarationSessionTestCase {

  @Test
  public void configLessConnectionLessOnOperation() {
    ComponentElementDeclaration elementDeclaration = configLessConnectionLessOPDeclaration(CONFIG_NAME);
    getResultAndValidate(session, elementDeclaration, PROVIDED_PARAMETER_NAME, "ConfigLessConnectionLessNoActingParameter");
  }

  @Test
  public void configLessOnOperation() {
    ComponentElementDeclaration elementDeclaration = configLessOPDeclaration(CONFIG_NAME);
    getResultAndValidate(session, elementDeclaration, PROVIDED_PARAMETER_NAME, CLIENT_NAME);
  }

  @Test
  public void actingParameterOnOperation() {
    final String actingParameter = "actingParameter";
    ComponentElementDeclaration elementDeclaration = actingParameterOPDeclaration(CONFIG_NAME, actingParameter);
    getResultAndValidate(session, elementDeclaration, PROVIDED_PARAMETER_NAME, WITH_ACTING_PARAMETER + actingParameter);
  }

  @Test
  public void actingParameterGroup() {
    final String stringValue = "stringValue";
    final int intValue = 0;
    final List<String> listValue = asList("one", "two", "three");
    ComponentElementDeclaration elementDeclaration =
        actingParameterGroupOPDeclaration(CONFIG_NAME, stringValue, intValue, listValue);
    getResultAndValidate(session, elementDeclaration, PROVIDED_PARAMETER_NAME, "stringValue-0-one-two-three");
  }

  @Test
  public void complexActingParameter() {
    final String stringValue = "stringValue";
    ComponentElementDeclaration elementDeclaration =
        complexActingParameterOPDeclaration(CONFIG_NAME, stringValue);
    getResultAndValidate(session, elementDeclaration, PROVIDED_PARAMETER_NAME, stringValue);
  }

  static void getResultAndValidate(DeclarationSession session, ParameterizedElementDeclaration elementDeclaration,
                                   String parameterName, String expectedValue) {
    ValueResult providerResult = session.getValues(elementDeclaration, parameterName);

    assertThat(providerResult.isSuccess(), equalTo(true));
    assertThat(providerResult.getValues(), hasSize(1));
    assertThat(providerResult.getValues().iterator().next().getId(), is(expectedValue));
  }

}
