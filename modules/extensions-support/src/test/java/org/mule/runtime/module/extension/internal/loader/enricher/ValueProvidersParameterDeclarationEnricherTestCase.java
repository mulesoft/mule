/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.core.api.config.MuleManifest.getProductVersion;
import static org.mule.runtime.module.extension.internal.loader.enricher.EnricherTestUtils.getNamedObject;

import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.java.DefaultJavaModelLoaderDelegate;
import org.mule.runtime.module.extension.internal.loader.java.property.DeclaringMemberModelProperty;
import org.mule.test.values.extension.ValuesExtension;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

public class ValueProvidersParameterDeclarationEnricherTestCase {

  private ExtensionDeclaration declaration;

  @Before
  public void setUp() {
    ExtensionDeclarer declarer = new DefaultJavaModelLoaderDelegate(ValuesExtension.class, getProductVersion())
        .declare(new DefaultExtensionLoadingContext(getClass().getClassLoader(), getDefault(emptySet())));
    new ValueProvidersParameterDeclarationEnricher()
        .enrich(new DefaultExtensionLoadingContext(declarer, this.getClass().getClassLoader(), getDefault(emptySet())));
    this.declaration = declarer.getDeclaration();
  }

  @Test
  public void verifyValueProviderWithoutParameters() {
    ParameterDeclaration parameterDeclaration = getParameterByOperationAndName("singleValuesEnabledParameter", "channels");

    assertThat(parameterDeclaration, notNullValue());
    assertThat(parameterDeclaration.getValueProviderModel(), notNullValue());
    assertThat(parameterDeclaration.getValueProviderModel().getActingParameters(), hasSize(0));
    assertThat(parameterDeclaration.getValueProviderModel().getParameters(), hasSize(0));
  }

  @Test
  public void verifyValueProviderWithRequiredParameter() {
    ParameterDeclaration parameterDeclaration = getParameterByOperationAndName("withRequiredParameter", "providedParameters");
    assertWithRequiredParameter(parameterDeclaration, "requiredValue");
  }

  @Test
  public void verifyValueProviderWithRequiredParameterWithAlias() {
    OperationDeclaration operationDeclaration =
        getNamedObject(this.declaration.getOperations(), "singleValuesWithRequiredParameterWithAlias");
    ParameterDeclaration parameterDeclaration = getNamedObject(operationDeclaration.getAllParameters(), "channels");
    ParameterDeclaration aliasedParameterDeclaration = getNamedObject(operationDeclaration.getAllParameters(), "superString");
    Optional<DeclaringMemberModelProperty> modelProperty =
        aliasedParameterDeclaration.getModelProperty(DeclaringMemberModelProperty.class);

    assertThat(modelProperty.isPresent(), is(true));
    assertThat(modelProperty.get().getDeclaringField(), notNullValue());
    assertThat(modelProperty.get().getDeclaringField().getName(), is("requiredString"));
    assertWithRequiredParameter(parameterDeclaration, "superString");
  }

  @Test
  public void verifyValueProviderWithOptionalParameter() {
    ParameterDeclaration parameterDeclaration = getParameterByOperationAndName("withOptionalParameter", "providedParameters");

    assertThat(parameterDeclaration, notNullValue());
    assertThat(parameterDeclaration.getValueProviderModel(), notNullValue());
    assertThat(parameterDeclaration.getValueProviderModel().getActingParameters(), hasSize(0));

    assertThat(parameterDeclaration.getValueProviderModel().getParameters(), hasSize(1));
    assertThat(parameterDeclaration.getValueProviderModel().getParameters(), contains(item("optionalValue", false)));
  }

  @Test
  public void verifyValueProviderWithRequiredAndOptionalParameters() {
    ParameterDeclaration parameterDeclaration =
        getParameterByOperationAndName("withRequiredAndOptionalParameters", "providedParameters");

    assertThat(parameterDeclaration, notNullValue());
    assertThat(parameterDeclaration.getValueProviderModel(), notNullValue());
    assertThat(parameterDeclaration.getValueProviderModel().getActingParameters(), hasSize(1));
    assertThat(parameterDeclaration.getValueProviderModel().getActingParameters(), contains("requiredValue"));

    assertThat(parameterDeclaration.getValueProviderModel().getParameters(), hasSize(2));
    assertThat(parameterDeclaration.getValueProviderModel().getParameters(),
               contains(item("requiredValue", true), item("optionalValue", false)));
  }

  @Test
  public void verifyParametersOfValueProviderModel() {
    ParameterDeclaration parameterDeclaration = getParameterByOperationAndName("withRequiredParameter", "providedParameters");

    assertThat(parameterDeclaration, notNullValue());
    assertThat(parameterDeclaration.getValueProviderModel(), notNullValue());
    assertThat(parameterDeclaration.getValueProviderModel().getParameters(), hasSize(1));

    ParameterModel parameter = parameterDeclaration.getValueProviderModel().getParameters().get(0);
    assertThat(parameter, notNullValue());
    assertThat(parameter.getName(), is("requiredValue"));
    assertThat(parameter.isRequired(), is(true));
    assertThat(parameter.hasDynamicType(), is(false));
    assertThat(parameter.isComponentId(), is(false));
    assertThat(parameter.isOverrideFromConfig(), is(false));
    assertThat(parameter.isDeprecated(), is(false));
    assertThat(parameter.getDisplayModel().isPresent(), is(false));
    assertThat(parameter.getDescription(), is(""));
    assertThat(parameter.getExpressionSupport(), nullValue());
    assertThat(parameter.getRole(), nullValue());
    assertThat(parameter.getAllowedStereotypes(), hasSize(0));
    assertThat(parameter.getModelProperties(), hasSize(0));
    assertThat(parameter.getDslConfiguration(), notNullValue());
    assertThat(parameter.getDefaultValue(), nullValue());
    assertThat(parameter.getValueProviderModel().isPresent(), is(false));
    assertThat(parameter.getDeprecationModel().isPresent(), is(false));
    assertThat(parameter.getType(), instanceOf(StringType.class));
  }

  @Test
  public void verifyDefaultValueOfValueProviderWithOptionalParameter() {
    assertDefaultValueOfParameter(getParameterByOperationAndName("withOptionalParameter", "providedParameters"));
    assertDefaultValueOfParameter(getParameterByOperationAndName("withOptionalParameterWithDefault", "providedParameters"));
    assertDefaultValueOfParameter(getParameterByOperationAndName("withVPOptionalParameterWithDefaultValue",
                                                                 "providedParameters"));
    assertDefaultValueOfParameter(getParameterByOperationAndName("withOptionalParameterAsRequired", "providedParameters"));
  }

  private void assertWithRequiredParameter(ParameterDeclaration parameterDeclaration, String parameterName) {
    assertThat(parameterDeclaration, notNullValue());
    assertThat(parameterDeclaration.getValueProviderModel(), notNullValue());
    assertThat(parameterDeclaration.getValueProviderModel().getActingParameters(), hasSize(1));
    assertThat(parameterDeclaration.getValueProviderModel().getActingParameters(), contains(parameterName));

    assertThat(parameterDeclaration.getValueProviderModel().getParameters(), hasSize(1));
    assertThat(parameterDeclaration.getValueProviderModel().getParameters(), contains(item(parameterName, true)));
  }

  private void assertDefaultValueOfParameter(ParameterDeclaration parameterDeclaration) {
    assertThat(parameterDeclaration, notNullValue());
    assertThat(parameterDeclaration.getValueProviderModel(), notNullValue());
    assertThat(parameterDeclaration.getValueProviderModel().getParameters(), hasSize(1));
    assertThat(parameterDeclaration.getValueProviderModel().getParameters(), contains(hasProperty("defaultValue", nullValue())));
  }

  private ParameterDeclaration getParameterByOperationAndName(String operationName, String parameterName) {
    OperationDeclaration operationDeclaration = getNamedObject(this.declaration.getOperations(), operationName);
    return getNamedObject(operationDeclaration.getAllParameters(), parameterName);
  }

  private org.hamcrest.Matcher<Object> item(String name, boolean required) {
    return allOf(hasProperty("name", is(name)), hasProperty("required", is(required)));
  }
}
