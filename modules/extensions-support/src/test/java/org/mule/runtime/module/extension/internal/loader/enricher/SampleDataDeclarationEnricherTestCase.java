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
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
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
import org.mule.test.data.sample.extension.SampleDataExtension;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

public class SampleDataDeclarationEnricherTestCase {

  private ExtensionDeclaration declaration;

  @Before
  public void setUp() {
    ExtensionDeclarer declarer = new DefaultJavaModelLoaderDelegate(SampleDataExtension.class, getProductVersion())
        .declare(new DefaultExtensionLoadingContext(getClass().getClassLoader(), getDefault(emptySet())));
    new SampleDataDeclarationEnricher()
        .enrich(new DefaultExtensionLoadingContext(declarer, this.getClass().getClassLoader(), getDefault(emptySet())));
    this.declaration = declarer.getDeclaration();
  }

  @Test
  public void verifySampleDataProviderWithoutParameters() {
    OperationDeclaration operationDeclaration = getNamedObject(this.declaration.getOperations(), "parameterLess");

    assertThat(operationDeclaration, notNullValue());
    assertThat(operationDeclaration.getSampleDataProviderModel(), notNullValue());
    assertThat(operationDeclaration.getSampleDataProviderModel().isPresent(), is(true));
    assertThat(operationDeclaration.getSampleDataProviderModel().get().getActingParameters(), hasSize(0));
    assertThat(operationDeclaration.getSampleDataProviderModel().get().getParameters(), hasSize(0));
  }

  @Test
  public void verifySampleDataProviderWithRequiredParameter() {
    OperationDeclaration operationDeclaration = getNamedObject(this.declaration.getOperations(), "connectionLess");

    assertWithRequiredParameter(operationDeclaration, new String[] {"payload", "attributes"});
  }

  @Test
  public void verifySampleDataProviderWithParameterWithAlias() {
    OperationDeclaration operationDeclaration =
        getNamedObject(this.declaration.getConfigurations().get(0).getOperations(), "aliasedGroup");

    assertAliasedParameter(operationDeclaration, "aliasedPayload", "payload");
    assertAliasedParameter(operationDeclaration, "aliasedAttributes", "attributes");
    assertWithRequiredParameter(operationDeclaration, new String[] {"aliasedPayload", "aliasedAttributes"});
  }

  private void assertAliasedParameter(OperationDeclaration operationDeclaration, String alias, String name) {
    ParameterDeclaration aliasedParameterDeclaration = getNamedObject(operationDeclaration.getAllParameters(), alias);
    Optional<DeclaringMemberModelProperty> modelProperty =
        aliasedParameterDeclaration.getModelProperty(DeclaringMemberModelProperty.class);
    assertThat(modelProperty.isPresent(), is(true));
    assertThat(modelProperty.get().getDeclaringField(), notNullValue());
    assertThat(modelProperty.get().getDeclaringField().getName(), is(name));
  }

  @Test
  public void verifyValueProviderWithOptionalParameter() {
    OperationDeclaration operationDeclaration = getNamedObject(this.declaration.getOperations(), "optionalParameters");

    assertThat(operationDeclaration, notNullValue());
    assertThat(operationDeclaration.getSampleDataProviderModel(), notNullValue());
    assertThat(operationDeclaration.getSampleDataProviderModel().isPresent(), is(true));
    assertThat(operationDeclaration.getSampleDataProviderModel().get().getActingParameters(), hasSize(0));
    assertThat(operationDeclaration.getSampleDataProviderModel().get().getParameters(), hasSize(2));
    assertThat(operationDeclaration.getSampleDataProviderModel().get().getParameters(),
               contains(item("payload", false), item("attributes", false)));
  }

  @Test
  public void verifyValueProviderWithRequiredAndOptionalParameters() {
    OperationDeclaration operationDeclaration =
        getNamedObject(this.declaration.getConfigurations().get(0).getOperations(), "parameterGroup");

    assertThat(operationDeclaration, notNullValue());
    assertThat(operationDeclaration.getSampleDataProviderModel(), notNullValue());
    assertThat(operationDeclaration.getSampleDataProviderModel().isPresent(), is(true));
    assertThat(operationDeclaration.getSampleDataProviderModel().get().getActingParameters(), hasSize(1));
    assertThat(operationDeclaration.getSampleDataProviderModel().get().getActingParameters(), contains("groupParameter"));
    assertThat(operationDeclaration.getSampleDataProviderModel().get().getParameters(), hasSize(2));
    assertThat(operationDeclaration.getSampleDataProviderModel().get().getParameters(),
               contains(item("groupParameter", true), item("optionalParameter", false)));
  }

  @Test
  public void verifyParametersOfValueProviderModel() {
    OperationDeclaration operationDeclaration = getNamedObject(this.declaration.getOperations(), "connectionLess");

    assertThat(operationDeclaration, notNullValue());
    assertThat(operationDeclaration.getSampleDataProviderModel().isPresent(), is(true));
    assertThat(operationDeclaration.getSampleDataProviderModel().get(), notNullValue());
    assertThat(operationDeclaration.getSampleDataProviderModel().get().getParameters(), hasSize(2));

    ParameterModel parameter = operationDeclaration.getSampleDataProviderModel().get().getParameters().get(0);
    assertThat(parameter, notNullValue());
    assertThat(parameter.getName(), is("payload"));
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
    assertDefaultValueOfParameter(getNamedObject(this.declaration.getOperations(), "optionalParameters"), "payload");
    assertDefaultValueOfParameter(getNamedObject(this.declaration.getOperations(), "optionalParameters"), "attributes");
    assertDefaultValueOfParameter(getNamedObject(this.declaration.getConfigurations().get(0).getOperations(), "parameterGroup"),
                                  "optionalParameter");
  }

  private void assertWithRequiredParameter(OperationDeclaration operationDeclaration, String[] parametersName) {
    assertThat(operationDeclaration, notNullValue());
    assertThat(operationDeclaration.getSampleDataProviderModel(), notNullValue());
    assertThat(operationDeclaration.getSampleDataProviderModel().isPresent(), is(true));
    assertThat(operationDeclaration.getSampleDataProviderModel().get().getActingParameters(), hasSize(2));
    assertThat(operationDeclaration.getSampleDataProviderModel().get().getActingParameters(), contains(parametersName));
    assertThat(operationDeclaration.getSampleDataProviderModel().get().getParameters(), hasSize(2));
    for (String parameterName : parametersName) {
      assertThat(operationDeclaration.getSampleDataProviderModel().get().getParameters(),
                 hasItem(both(hasProperty("name", is(parameterName))).and(hasProperty("required", is(true)))));
    }
  }

  private void assertDefaultValueOfParameter(OperationDeclaration operationDeclaration, String parameterName) {
    assertThat(operationDeclaration, notNullValue());
    assertThat(operationDeclaration.getSampleDataProviderModel().isPresent(), is(true));
    assertThat(operationDeclaration.getSampleDataProviderModel().get(), notNullValue());
    assertThat(operationDeclaration.getSampleDataProviderModel().get().getParameters(), hasSize(greaterThan(0)));
    Optional<ParameterModel> parameter = operationDeclaration.getSampleDataProviderModel().get().getParameters().stream()
        .filter(item -> !item.getName().equals(parameterName)).findFirst();
    assertThat(parameter.isPresent(), is(true));
    assertThat(parameter.get(), hasProperty("defaultValue", nullValue()));
  }

  private ParameterDeclaration getParameterByOperationAndName(String operationName, String parameterName) {
    OperationDeclaration operationDeclaration = getNamedObject(this.declaration.getOperations(), operationName);
    return getNamedObject(operationDeclaration.getAllParameters(), parameterName);
  }

  private org.hamcrest.Matcher<Object> item(String name, boolean required) {
    return allOf(hasProperty("name", is(name)), hasProperty("required", is(required)));
  }
}
