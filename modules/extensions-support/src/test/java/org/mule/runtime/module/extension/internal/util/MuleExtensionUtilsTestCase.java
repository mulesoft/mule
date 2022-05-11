/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;


import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.checkParameterGroupExclusiveness;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.extractExpression;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getDefaultValue;
import static org.mule.test.allure.AllureConstants.ArtifactAst.ARTIFACT_AST;
import static org.mule.test.allure.AllureConstants.ArtifactAst.ParameterAst.PARAMETER_AST;

import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ExclusiveParametersModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionParameterDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.runtime.ValueResolvingException;
import org.mule.runtime.module.extension.internal.runtime.resolver.ObjectBuilderValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(ARTIFACT_AST)
@Story(PARAMETER_AST)
public class MuleExtensionUtilsTestCase extends AbstractMuleTestCase {

  private static final String PAYLOAD_EXPRESSION = "#[payload]";
  private static final String MALFORMED_EXPRESSION = "#[payload";
  private static final String DEFAULT_VALUE = "DEFAULT_VALUE";

  @Rule
  public ExpectedException expectedException = none();

  @Test
  @Description("Parse mule expression")
  public void extractMuleExpression() {
    Optional<String> expressionValue = extractExpression(PAYLOAD_EXPRESSION);
    assertThat(expressionValue, is(of("payload")));
  }

  @Test
  @Description("Try parse malformed mule expression")
  public void parseMalformedMuleExpression() {
    Optional<String> expressionValue = extractExpression(MALFORMED_EXPRESSION);
    assertThat(expressionValue, is(empty()));
  }

  @Test
  public void deprecatedOptionalWithDefaultValue() {
    org.mule.runtime.extension.api.annotation.param.Optional optional =
        mock(org.mule.runtime.extension.api.annotation.param.Optional.class);
    when(optional.defaultValue()).thenReturn(DEFAULT_VALUE);
    assertOptional(getDefaultValue(optional));
  }

  @Test
  public void optionalWithDefaultValue() {
    org.mule.sdk.api.annotation.param.Optional optional = mock(org.mule.sdk.api.annotation.param.Optional.class);
    when(optional.defaultValue()).thenReturn(DEFAULT_VALUE);
    assertOptional(getDefaultValue(optional));
  }

  @Test
  public void parameterGroupExclusivenessForDslTrueSomeParameter() throws ConfigurationException, ValueResolvingException {
    Map<String, ValueResolver<?>> someParameterResolvers = new HashMap<>();
    someParameterResolvers.put("someParameter", mock(StaticValueResolver.class));
    ObjectBuilderValueResolver someParameter = mock(ObjectBuilderValueResolver.class);
    when(someParameter.getParameters()).thenReturn(someParameterResolvers);

    Map<String, ObjectBuilderValueResolver> parameters = new HashMap<>();
    parameters.put("oneParameterGroup", someParameter);
    checkParameterGroupExclusiveness(Optional.of(mock(OperationModel.class)), getParameterGroupModels(true), parameters,
                                     emptyMap());
  }

  @Test
  public void parameterGroupExclusivenessForDslTrueRepeatedNameParameter()
      throws ConfigurationException, ValueResolvingException {
    Map<String, ValueResolver<?>> repeatedNameParameterResolvers = new HashMap<>();
    repeatedNameParameterResolvers.put("repeatedNameParameter", mock(StaticValueResolver.class));
    ObjectBuilderValueResolver repeatedNameParameter = mock(ObjectBuilderValueResolver.class);
    when(repeatedNameParameter.getParameters()).thenReturn(repeatedNameParameterResolvers);

    Map<String, ValueResolver<?>> pojoParameterResolvers = new HashMap<>();
    pojoParameterResolvers.put("repeatedNameParameter", mock(StaticValueResolver.class));
    pojoParameterResolvers.put("anotherParameter", mock(StaticValueResolver.class));
    ObjectBuilderValueResolver pojoParameter = mock(ObjectBuilderValueResolver.class);
    when(pojoParameter.getParameters()).thenReturn(pojoParameterResolvers);

    Map<String, ObjectBuilderValueResolver> parameters = new HashMap<>();
    parameters.put("pojoParameter", pojoParameter);
    parameters.put("oneParameterGroup", repeatedNameParameter);
    checkParameterGroupExclusiveness(Optional.of(mock(OperationModel.class)), getParameterGroupModels(true), parameters,
                                     emptyMap());
  }

  @Test
  public void parameterGroupExclusivenessForDslFalseSomeParameter() throws ConfigurationException {
    Map<String, StaticValueResolver> parameters = new HashMap<>();
    parameters.put("someParameter", mock(StaticValueResolver.class));
    checkParameterGroupExclusiveness(Optional.of(mock(OperationModel.class)), getParameterGroupModels(false), parameters,
                                     emptyMap());
  }

  @Test
  public void parameterGroupExclusivenessForDslFalseComplexParameterDynamic()
      throws ConfigurationException, ValueResolvingException {
    Map<String, ValueResolver<?>> complexParameterResolvers = new HashMap<>();
    complexParameterResolvers.put("anotherParameter", mock(StaticValueResolver.class));
    complexParameterResolvers.put("repeatedNameParameter", mock(StaticValueResolver.class));
    ObjectBuilderValueResolver complexParameter = mock(ObjectBuilderValueResolver.class);
    when(complexParameter.getParameters()).thenReturn(complexParameterResolvers);

    Map<String, ObjectBuilderValueResolver> parameters = new HashMap<>();
    parameters.put("complexParameter", complexParameter);
    checkParameterGroupExclusiveness(Optional.of(mock(OperationModel.class)), getParameterGroupModels(false), parameters,
                                     emptyMap());
  }

  @Test
  public void parameterGroupExclusivenessForDslFalseRepeatedNameParameter()
      throws ConfigurationException, ValueResolvingException {
    Map<String, ValueResolver<?>> pojoParameterResolvers = new HashMap<>();
    pojoParameterResolvers.put("repeatedNameParameter", mock(StaticValueResolver.class));
    pojoParameterResolvers.put("anotherParameter", mock(StaticValueResolver.class));
    ObjectBuilderValueResolver pojoParameter = mock(ObjectBuilderValueResolver.class);
    when(pojoParameter.getParameters()).thenReturn(pojoParameterResolvers);

    Map<String, ValueResolver> parameters = new HashMap<>();
    parameters.put("repeatedNameParameter", mock(StaticValueResolver.class));
    parameters.put("pojoParameter", pojoParameter);
    checkParameterGroupExclusiveness(Optional.of(mock(OperationModel.class)), getParameterGroupModels(false), parameters,
                                     emptyMap());
  }

  @Test
  public void parameterGroupExclusivenessDslFalseComplexParameterDynamicAndPojoParameter()
      throws ConfigurationException, ValueResolvingException {
    Map<String, ValueResolver<?>> pojoParameterResolvers = new HashMap<>();
    pojoParameterResolvers.put("repeatedNameParameter", mock(StaticValueResolver.class));
    pojoParameterResolvers.put("anotherParameter", mock(StaticValueResolver.class));
    ObjectBuilderValueResolver pojoParameter = mock(ObjectBuilderValueResolver.class);
    when(pojoParameter.getParameters()).thenReturn(pojoParameterResolvers);

    Map<String, ObjectBuilderValueResolver> parameters = new HashMap<>();
    parameters.put("pojoParameter", pojoParameter);
    parameters.put("complexParameter", pojoParameter);
    checkParameterGroupExclusiveness(Optional.of(mock(OperationModel.class)), getParameterGroupModels(false), parameters,
                                     emptyMap());
  }

  @Test
  public void parameterGroupNameMissingAmongResolvedParameterNamesWithDslTrue() throws ConfigurationException {
    expectedException.expect(ConfigurationException.class);
    expectedException.expectMessage("Was expecting a parameter with name [oneParameterGroup] among the resolved parameters");
    Map<String, StaticValueResolver> parameters = new HashMap<>();
    parameters.put("someParameter", mock(StaticValueResolver.class));
    checkParameterGroupExclusiveness(Optional.of(mock(OperationModel.class)), getParameterGroupModels(true), parameters,
                                     emptyMap());
  }

  @Test
  public void moreThanOneParameterSetInGroup() throws ConfigurationException, ValueResolvingException {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage("In operation 'null', the following parameters cannot be set at the same time: [someParameter, complexParameter]");
    Map<String, StaticValueResolver> parameters = new HashMap<>();
    parameters.put("someParameter", mock(StaticValueResolver.class));
    parameters.put("complexParameter", mock(StaticValueResolver.class));
    checkParameterGroupExclusiveness(Optional.of(mock(OperationModel.class)), getParameterGroupModels(false), parameters,
                                     emptyMap());
  }

  @Test
  public void noParametersSetInGroup() throws ConfigurationException, ValueResolvingException {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage("Parameter group 'null' requires that one of its optional parameters should be set but all of them are missing. One of the following should be set: [someParameter, repeatedNameParameter, complexParameter]");
    checkParameterGroupExclusiveness(Optional.of(mock(OperationModel.class)), getParameterGroupModels(false), emptyMap(),
                                     emptyMap());
  }


  @Test
  public void parameterGroupExclusivenessForDslFalseWithSimpleParameterWithAlias()
      throws ConfigurationException, ValueResolvingException {
    Map<String, StaticValueResolver> parameters = new HashMap<>();
    parameters.put("some-parameter-alias", mock(StaticValueResolver.class));
    checkParameterGroupExclusiveness(Optional.of(mock(OperationModel.class)),
                                     getParameterGroupModelsWithAlias(false), parameters,
                                     getAliasedParameters());
  }

  @Test
  public void parameterGroupExclusivenessForDslFalseWithComplexParameterWithAlias()
      throws ConfigurationException, ValueResolvingException {
    Map<String, ValueResolver<?>> pojoParameterResolvers = new HashMap<>();
    pojoParameterResolvers.put("repeatedNameParameter", mock(StaticValueResolver.class));
    pojoParameterResolvers.put("anotherParameter", mock(StaticValueResolver.class));
    ObjectBuilderValueResolver pojoParameter = mock(ObjectBuilderValueResolver.class);
    when(pojoParameter.getParameters()).thenReturn(pojoParameterResolvers);
    Map<String, ObjectBuilderValueResolver> parameters = new HashMap<>();
    parameters.put("complex-parameter-alias", pojoParameter);
    checkParameterGroupExclusiveness(Optional.of(mock(OperationModel.class)),
                                     getParameterGroupModelsWithAlias(false), parameters,
                                     getAliasedParameters());
  }

  @Test
  public void parameterGroupExclusivenessForDslFalseWithMultipleParametersWithAlias()
      throws ConfigurationException, ValueResolvingException {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage("In operation 'null', the following parameters cannot be set at the same time: [some-parameter-alias, complex-parameter-alias]");
    Map<String, ValueResolver<?>> pojoParameterResolvers = new HashMap<>();
    pojoParameterResolvers.put("repeatedNameParameter", mock(StaticValueResolver.class));
    pojoParameterResolvers.put("anotherParameter", mock(StaticValueResolver.class));
    ObjectBuilderValueResolver pojoParameter = mock(ObjectBuilderValueResolver.class);
    when(pojoParameter.getParameters()).thenReturn(pojoParameterResolvers);
    Map<String, ValueResolver> parameters = new HashMap<>();
    parameters.put("some-parameter-alias", mock(StaticValueResolver.class));
    parameters.put("complex-parameter-alias", pojoParameter);
    checkParameterGroupExclusiveness(Optional.of(mock(OperationModel.class)),
                                     getParameterGroupModelsWithAlias(false), parameters,
                                     getAliasedParameters());
  }

  private void assertOptional(Optional<String> defaultValue) {
    assertThat(defaultValue.isPresent(), is(true));
    assertThat(defaultValue.get(), is(DEFAULT_VALUE));
  }

  private Map<String, String> getAliasedParameters() {
    Map<String, String> aliasedParameters = new HashMap<>();
    aliasedParameters.put("someParameter", "some-parameter-alias");
    aliasedParameters.put("repeatedNameParameter", "repeated-nameParameter-alias");
    aliasedParameters.put("complexParameter", "complex-parameter-alias");
    return aliasedParameters;
  }

  private List<ParameterGroupModel> getParameterGroupModels(boolean showInDsl) {
    return getParameterGroupModels(showInDsl, "someParameter", "repeatedNameParameter", "complexParameter");
  }

  private List<ParameterGroupModel> getParameterGroupModelsWithAlias(boolean showInDsl) {
    return getParameterGroupModels(showInDsl, "some-parameter-alias", "repeated-nameParameter-alias", "complex-parameter-alias");
  }

  private List<ParameterGroupModel> getParameterGroupModels(boolean showInDsl, String... parameters) {
    ExclusiveParametersModel exclusiveParametersModel = mock(ExclusiveParametersModel.class);
    when(exclusiveParametersModel.getExclusiveParameterNames())
        .thenReturn(new HashSet<>(asList(parameters)));
    when(exclusiveParametersModel.isOneRequired()).thenReturn(true);

    ParameterGroupModel parameterGroupModel = mock(ParameterGroupModel.class);
    when(parameterGroupModel.getExclusiveParametersModels()).thenReturn(singletonList(exclusiveParametersModel));
    when(parameterGroupModel.isShowInDsl()).thenReturn(showInDsl);

    ExtensionParameter extensionParameter = mock(ExtensionParameter.class);
    when(extensionParameter.getName()).thenReturn("oneParameterGroup");
    ExtensionParameterDescriptorModelProperty property = mock(ExtensionParameterDescriptorModelProperty.class);
    when(property.getExtensionParameter()).thenReturn(extensionParameter);
    when(parameterGroupModel.getModelProperty(ExtensionParameterDescriptorModelProperty.class)).thenReturn(of(property));

    return singletonList(parameterGroupModel);
  }
}
