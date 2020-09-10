/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.metadata.resolving.FailureCode.COMPONENT_NOT_FOUND;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newParameterGroup;
import static org.mule.runtime.extension.api.values.ValueResolvingException.INVALID_VALUE_RESOLVER_NAME;
import static org.mule.runtime.extension.api.values.ValueResolvingException.MISSING_REQUIRED_PARAMETERS;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.ACTING_PARAMETER_NAME;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.actingParameterGroupOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.actingParameterGroupOPWithAliasDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.actingParameterOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.complexActingParameterOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.complexParameterValue;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.configLessConnectionLessOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.configLessOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.innerPojo;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.parameterValueProviderWithConfig;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.sourceWithMultiLevelValue;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.api.value.ValueResult;
import org.mule.runtime.app.declaration.api.ComponentElementDeclaration;
import org.mule.runtime.app.declaration.api.OperationElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterValue;
import org.mule.runtime.app.declaration.api.fluent.ElementDeclarer;

import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;

import org.junit.Test;

public class ComponentValueProviderTestCase extends DeclarationSessionTestCase {

  @Test
  public void configLessConnectionLessOnOperation() {
    ComponentElementDeclaration elementDeclaration = configLessConnectionLessOPDeclaration(CONFIG_NAME);
    validateValuesSuccess(session, elementDeclaration, PROVIDED_PARAMETER_NAME, "ConfigLessConnectionLessNoActingParameter");
  }

  @Test
  public void configLessConnectionLessOnOperationWithMissingConfigWorks() {
    ComponentElementDeclaration elementDeclaration = configLessConnectionLessOPDeclaration("");
    validateValuesSuccess(session, elementDeclaration, PROVIDED_PARAMETER_NAME, "ConfigLessConnectionLessNoActingParameter");
  }

  @Test
  public void configLessOnOperation() {
    ComponentElementDeclaration elementDeclaration = configLessOPDeclaration(CONFIG_NAME);
    validateValuesSuccess(session, elementDeclaration, PROVIDED_PARAMETER_NAME, CLIENT_NAME);
  }

  @Test
  public void configLessOnOperationFailsWithMissingConfig() {
    ComponentElementDeclaration elementDeclaration = configLessOPDeclaration("");
    validateValuesFailure(session,
                          elementDeclaration,
                          PROVIDED_PARAMETER_NAME,
                          "The value provider requires a connection and none was provided",
                          MISSING_REQUIRED_PARAMETERS);
  }

  @Test
  public void actingParameterOnOperation() {
    final String actingParameter = "actingParameter";
    ComponentElementDeclaration elementDeclaration = actingParameterOPDeclaration(CONFIG_NAME, actingParameter);
    validateValuesSuccess(session, elementDeclaration, PROVIDED_PARAMETER_NAME, WITH_ACTING_PARAMETER + actingParameter);
  }

  @Test
  public void missingActingParameterFails() {
    final String actingParameter = "actingParameter";
    ComponentElementDeclaration elementDeclaration = actingParameterOPDeclaration(CONFIG_NAME, actingParameter);
    elementDeclaration.getParameterGroups().get(0).getParameters().remove(0);
    validateValuesFailure(session, elementDeclaration, PROVIDED_PARAMETER_NAME,
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
    validateValuesSuccess(session, elementDeclaration, PROVIDED_PARAMETER_NAME, "stringValue-0-one-two-three");
  }

  @Test
  public void actingParameterGroupWithAlias() {
    final String stringValue = "stringValue";
    final int intValue = 0;
    final List<String> listValue = asList("one", "two", "three");
    ComponentElementDeclaration elementDeclaration =
        actingParameterGroupOPWithAliasDeclaration(CONFIG_NAME, stringValue, intValue, listValue);
    validateValuesSuccess(session, elementDeclaration, PROVIDED_PARAMETER_NAME, "stringValue-0-one-two-three");
  }

  @Test
  public void missingParameterFromParameterGroupFails() {
    final String stringValue = "stringValue";
    final int intValue = 0;
    final List<String> listValue = asList("one", "two", "three");

    ComponentElementDeclaration elementDeclaration =
        actingParameterGroupOPDeclaration(CONFIG_NAME, stringValue, intValue, listValue);
    elementDeclaration.getParameterGroups().get(0).getParameters().remove(1);
    validateValuesFailure(session, elementDeclaration, PROVIDED_PARAMETER_NAME,
                          "Unable to retrieve values. There are missing required parameters for the resolution: [intParam]",
                          MISSING_REQUIRED_PARAMETERS);

    elementDeclaration =
        actingParameterGroupOPDeclaration(CONFIG_NAME, stringValue, intValue, listValue);
    elementDeclaration.getParameterGroups().get(0).getParameters().remove(2);
    validateValuesFailure(session, elementDeclaration, PROVIDED_PARAMETER_NAME,
                          "Unable to retrieve values. There are missing required parameters for the resolution: [listParams]",
                          MISSING_REQUIRED_PARAMETERS);

    elementDeclaration =
        actingParameterGroupOPDeclaration(CONFIG_NAME, stringValue, intValue, listValue);
    elementDeclaration.getParameterGroups().get(0).getParameters().remove(1);
    elementDeclaration.getParameterGroups().get(0).getParameters().remove(1);
    validateValuesFailure(session, elementDeclaration, PROVIDED_PARAMETER_NAME,
                          "Unable to retrieve values. There are missing required parameters for the resolution: [intParam, listParams]",
                          MISSING_REQUIRED_PARAMETERS);
  }

  @Test
  public void complexActingParameter() {
    int intParam = 0;
    String stringParam = "zero";
    List<String> listParam = asList("zero", "one", "two");
    Map<String, String> mapParam = ImmutableMap.of("0", "zero", "1", "one", "2", "two");
    ParameterValue innerPojoValue = innerPojo(intParam, stringParam, listParam, mapParam);
    List<ParameterValue> complexListParam = asList(innerPojoValue);
    Map<String, ParameterValue> complexMapParam = ImmutableMap.of("0", innerPojoValue, "1", innerPojoValue);
    ComponentElementDeclaration elementDeclaration =
        complexActingParameterOPDeclaration(CONFIG_NAME,
                                            complexParameterValue(intParam, stringParam, listParam, mapParam, innerPojoValue,
                                                                  complexListParam, complexMapParam));
    String innerPojoStringValue = intParam +
        stringParam +
        "zeroonetwo" + //listParam
        "0zero1one2two"; //mapParam

    String expectedValue = intParam +
        stringParam +
        "zeroonetwo" + //listParam
        "0zero1one2two" + //mapParam
        innerPojoStringValue + //all inner pojo parameters
        innerPojoStringValue + //complex list with 1 inner pojo
        "0" + innerPojoStringValue + "1" + innerPojoStringValue; //complexMap
    validateValuesSuccess(session, elementDeclaration, PROVIDED_PARAMETER_NAME, expectedValue);
  }

  @Test
  public void getValuesWithWrongDeclaration() {
    final ElementDeclarer declarer = ElementDeclarer.forExtension("WrongExtension");
    OperationElementDeclaration operationElementDeclaration =
        declarer.newOperation("someOperation")
            .withParameterGroup(newParameterGroup()
                .withParameter("someParameter", "value")
                .getDeclaration())
            .getDeclaration();
    validateValuesFailure(session, operationElementDeclaration, "anyParameter",
                          "ElementDeclaration is defined for extension: 'WrongExtension' which is not part of the context: '[mule, ToolingSupportTest, module]'",
                          COMPONENT_NOT_FOUND.getName());
  }

  @Test
  public void missingConfigRef() {
    OperationElementDeclaration operationElementDeclaration = parameterValueProviderWithConfig(null);
    validateValuesFailure(session, operationElementDeclaration, PROVIDED_PARAMETER_NAME,
                          "The value provider requires a configuration and none was provided",
                          MISSING_REQUIRED_PARAMETERS);
  }

  @Test
  public void notFoundConfigRef() {
    OperationElementDeclaration operationElementDeclaration = parameterValueProviderWithConfig("missing_config_ref");
    validateValuesFailure(session, operationElementDeclaration, PROVIDED_PARAMETER_NAME,
                          "The provider requires a configuration but the one referenced by element declaration with name: 'missing_config_ref' is not present",
                          COMPONENT_NOT_FOUND.getName());
  }


  @Test
  public void getValuesOnParameterWithNoValueProvider() {
    final String actingParameter = "actingParameter";
    ComponentElementDeclaration elementDeclaration = actingParameterOPDeclaration(CONFIG_NAME, actingParameter);
    validateValuesFailure(session, elementDeclaration, ACTING_PARAMETER_NAME,
                          "Unable to find model for parameter or parameter group with name 'actingParameter'.",
                          INVALID_VALUE_RESOLVER_NAME);
  }

  @Test
  public void multiLevelSourceValue() {
    ComponentElementDeclaration elementDeclaration = sourceWithMultiLevelValue(CONFIG_NAME, "America", "USA");
    // ProviderName is groupName
    ValueResult valueResult = getValueResult(session, elementDeclaration, "values");
    assertThat(valueResult.isSuccess(), is(true));
    // ValueResult for multi level no matter if previous levels are set it would always return the whole tree
    assertThat(valueResult.getValues(), hasSize(1));
    Value america = valueResult.getValues().stream().findFirst().get();
    assertThat(america.getId(), is("America"));
    assertThat(america.getChilds(), hasSize(2));
  }
}
