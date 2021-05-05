/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;


import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getDefaultValue;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.extractExpression;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.checkParameterGroupExclusiveness;
import static org.mule.test.allure.AllureConstants.ArtifactAst.ARTIFACT_AST;
import static org.mule.test.allure.AllureConstants.ArtifactAst.ParameterAst.PARAMETER_AST;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ExclusiveParametersModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.core.api.config.ConfigurationException;
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
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(ARTIFACT_AST)
@Story(PARAMETER_AST)
public class MulExtensionUtilsTestCase extends AbstractMuleTestCase {

  private static final String PAYLOAD_EXPRESSION = "#[payload]";
  private static final String MALFORMED_EXPRESSION = "#[payload";
  private static final String DEFAULT_VALUE = "DEFAULT_VALUE";

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
  public void parameterGroupExclusivenessForDslTrueSimpleParameter() throws ConfigurationException, ValueResolvingException {
    ExclusiveParametersModel exclusiveParametersModel = mock(ExclusiveParametersModel.class);
    when(exclusiveParametersModel.getExclusiveParameterNames())
        .thenReturn(new HashSet<>(asList("someParameter", "repeatedNameParameter", "complexParameter")));

    ParameterGroupModel parameterGroupModel = mock(ParameterGroupModel.class);
    when(parameterGroupModel.getExclusiveParametersModels()).thenReturn(singletonList(exclusiveParametersModel));
    when(parameterGroupModel.isShowInDsl()).thenReturn(true);
    List<ParameterGroupModel> groups = singletonList(parameterGroupModel);

    Map<String, ValueResolver<?>> parameterResolvers = new HashMap<>();
    parameterResolvers.put("someParameter", mock(StaticValueResolver.class));
    ObjectBuilderValueResolver valueResolver = mock(ObjectBuilderValueResolver.class);
    when(valueResolver.getParameters()).thenReturn(parameterResolvers);
    Map<String, ObjectBuilderValueResolver> parameters = new HashMap<>();
    parameters.put("oneParameterGroup", valueResolver);
    Map<String, String> aliasedParameterNames = new HashMap<>(0);
    checkParameterGroupExclusiveness(Optional.of(mock(OperationModel.class)), groups, parameters, aliasedParameterNames);
  }

  @Test
  public void parameterGroupExclusivenessForDslTrueRepeatedNameParameter()
      throws ConfigurationException, ValueResolvingException {
    ExclusiveParametersModel exclusiveParametersModel = mock(ExclusiveParametersModel.class);
    when(exclusiveParametersModel.getExclusiveParameterNames())
        .thenReturn(new HashSet<>(asList("someParameter", "repeatedNameParameter", "complexParameter")));

    ParameterGroupModel parameterGroupModel = mock(ParameterGroupModel.class);
    when(parameterGroupModel.getExclusiveParametersModels()).thenReturn(singletonList(exclusiveParametersModel));
    when(parameterGroupModel.isShowInDsl()).thenReturn(true);
    List<ParameterGroupModel> groups = singletonList(parameterGroupModel);

    Map<String, ValueResolver<?>> parameterResolvers = new HashMap<>();
    parameterResolvers.put("repeatedNameParameter", mock(StaticValueResolver.class));
    ObjectBuilderValueResolver valueResolver = mock(ObjectBuilderValueResolver.class);
    when(valueResolver.getParameters()).thenReturn(parameterResolvers);
    Map<String, ObjectBuilderValueResolver> parameters = new HashMap<>();
    parameters.put("oneParameterGroup", valueResolver);
    Map<String, String> aliasedParameterNames = new HashMap<>(0);
    checkParameterGroupExclusiveness(Optional.of(mock(OperationModel.class)), groups, parameters, aliasedParameterNames);
  }

  @Test
  public void parameterGroupExclusivenessDslTrueComplexParameter() throws ConfigurationException, ValueResolvingException {
    ExclusiveParametersModel exclusiveParametersModel = mock(ExclusiveParametersModel.class);
    when(exclusiveParametersModel.getExclusiveParameterNames())
        .thenReturn(new HashSet<>(asList("someParameter", "repeatedNameParameter", "complexParameter")));

    ParameterGroupModel parameterGroupModel = mock(ParameterGroupModel.class);
    when(parameterGroupModel.getExclusiveParametersModels()).thenReturn(singletonList(exclusiveParametersModel));
    when(parameterGroupModel.isShowInDsl()).thenReturn(true);
    List<ParameterGroupModel> groups = singletonList(parameterGroupModel);

    Map<String, ValueResolver<?>> parameterResolvers = new HashMap<>();
    parameterResolvers.put("complexParameter", mock(StaticValueResolver.class));
    ObjectBuilderValueResolver valueResolver = mock(ObjectBuilderValueResolver.class);
    when(valueResolver.getParameters()).thenReturn(parameterResolvers);
    Map<String, ObjectBuilderValueResolver> parameters = new HashMap<>();
    parameters.put("oneParameterGroup", valueResolver);
    Map<String, String> aliasedParameterNames = new HashMap<>(0);
    checkParameterGroupExclusiveness(Optional.of(mock(OperationModel.class)), groups, parameters, aliasedParameterNames);
  }

  @Test
  public void parameterGroupExclusivenessForDslFalseSimpleParameter() throws ConfigurationException {
    ExclusiveParametersModel exclusiveParametersModel = mock(ExclusiveParametersModel.class);
    when(exclusiveParametersModel.getExclusiveParameterNames())
        .thenReturn(new HashSet<>(asList("someParameter", "repeatedNameParameter", "complexParameter")));

    ParameterGroupModel parameterGroupModel = mock(ParameterGroupModel.class);
    when(parameterGroupModel.getExclusiveParametersModels()).thenReturn(singletonList(exclusiveParametersModel));
    when(parameterGroupModel.isShowInDsl()).thenReturn(false);
    List<ParameterGroupModel> groups = singletonList(parameterGroupModel);

    Map<String, StaticValueResolver> parameters = new HashMap<>();
    parameters.put("someParameter", mock(StaticValueResolver.class));
    Map<String, String> aliasedParameterNames = new HashMap<>(0);
    checkParameterGroupExclusiveness(Optional.of(mock(OperationModel.class)), groups, parameters, aliasedParameterNames);
  }

  @Test
  public void parameterGroupExclusivenessForDslFalseRepeatedNameParameter() throws ConfigurationException {
    ExclusiveParametersModel exclusiveParametersModel = mock(ExclusiveParametersModel.class);
    when(exclusiveParametersModel.getExclusiveParameterNames())
        .thenReturn(new HashSet<>(asList("someParameter", "repeatedNameParameter", "complexParameter")));

    ParameterGroupModel parameterGroupModel = mock(ParameterGroupModel.class);
    when(parameterGroupModel.getExclusiveParametersModels()).thenReturn(singletonList(exclusiveParametersModel));
    when(parameterGroupModel.isShowInDsl()).thenReturn(false);
    List<ParameterGroupModel> groups = singletonList(parameterGroupModel);

    Map<String, StaticValueResolver> parameters = new HashMap<>();
    parameters.put("repeatedNameParameter", mock(StaticValueResolver.class));
    Map<String, String> aliasedParameterNames = new HashMap<>(0);
    checkParameterGroupExclusiveness(Optional.of(mock(OperationModel.class)), groups, parameters, aliasedParameterNames);
  }

  @Test
  public void parameterGroupExclusivenessForDslFalseComplexParameter() throws ConfigurationException {
    ExclusiveParametersModel exclusiveParametersModel = mock(ExclusiveParametersModel.class);
    when(exclusiveParametersModel.getExclusiveParameterNames())
        .thenReturn(new HashSet<>(asList("someParameter", "repeatedNameParameter", "complexParameter")));

    ParameterGroupModel parameterGroupModel = mock(ParameterGroupModel.class);
    when(parameterGroupModel.getExclusiveParametersModels()).thenReturn(singletonList(exclusiveParametersModel));
    when(parameterGroupModel.isShowInDsl()).thenReturn(false);
    List<ParameterGroupModel> groups = singletonList(parameterGroupModel);

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("complexParameter", mock(ObjectBuilderValueResolver.class));
    Map<String, String> aliasedParameterNames = new HashMap<>(0);
    checkParameterGroupExclusiveness(Optional.of(mock(OperationModel.class)), groups, parameters, aliasedParameterNames);
  }

  @Test
  public void parameterGroupExclusivenessForDslFalseComplexParameterDynamic()
      throws ConfigurationException, ValueResolvingException {
    ExclusiveParametersModel exclusiveParametersModel = mock(ExclusiveParametersModel.class);
    when(exclusiveParametersModel.getExclusiveParameterNames())
        .thenReturn(new HashSet<>(asList("someParameter", "repeatedNameParameter", "complexParameter")));

    ParameterGroupModel parameterGroupModel = mock(ParameterGroupModel.class);
    when(parameterGroupModel.getExclusiveParametersModels()).thenReturn(singletonList(exclusiveParametersModel));
    when(parameterGroupModel.isShowInDsl()).thenReturn(false);
    List<ParameterGroupModel> groups = singletonList(parameterGroupModel);

    Map<String, ValueResolver<?>> parameterResolvers = new HashMap<>();
    parameterResolvers.put("anotherParameter", mock(StaticValueResolver.class));
    parameterResolvers.put("repeatedNameParameter", mock(StaticValueResolver.class));
    ObjectBuilderValueResolver valueResolver = mock(ObjectBuilderValueResolver.class);
    when(valueResolver.getParameters()).thenReturn(parameterResolvers);
    Map<String, ObjectBuilderValueResolver> parameters = new HashMap<>();
    parameters.put("complexParameter", valueResolver);
    Map<String, String> aliasedParameterNames = new HashMap<>(0);
    checkParameterGroupExclusiveness(Optional.of(mock(OperationModel.class)), groups, parameters, aliasedParameterNames);
  }

  private void assertOptional(Optional<String> defaultValue) {
    assertThat(defaultValue.isPresent(), is(true));
    assertThat(defaultValue.get(), is(DEFAULT_VALUE));
  }
}
