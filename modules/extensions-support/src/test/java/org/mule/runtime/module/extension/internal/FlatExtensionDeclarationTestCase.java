/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.extension.api.Category.COMMUNITY;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.REQUIRED;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.SUPPORTED;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.ADDRESS;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.ARG_LESS;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.BROADCAST;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.BROADCAST_DESCRIPTION;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.CALLBACK;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.CALLBACK_DESCRIPTION;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.CONFIG_DESCRIPTION;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.CONFIG_NAME;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.CONNECTION_PROVIDER_CONNECTOR_TYPE;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.CONNECTION_PROVIDER_DESCRIPTION;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.CONNECTION_PROVIDER_NAME;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.CONSUMER;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.DEFAULT_PORT;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.GO_GET_THEM_TIGER;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.HAS_NO_ARGS;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.LISTENER;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.LISTEN_DESCRIPTION;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.MTOM_DESCRIPTION;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.MTOM_ENABLED;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.MULESOFT;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.OPERATION;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.PASSWORD;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.PASSWORD_DESCRIPTION;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.PORT;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.PORT_DESCRIPTION;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.SERVICE;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.SERVICE_ADDRESS;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.SERVICE_NAME;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.SERVICE_PORT;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.THE_OPERATION_TO_USE;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.URI_TO_FIND_THE_WSDL;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.URL;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.URL_DESCRIPTION;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.USERNAME;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.USERNAME_DESCRIPTION;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.VERSION;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.WSDL_LOCATION;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.WS_CONSUMER;
import static org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer.WS_CONSUMER_DESCRIPTION;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.TARGET_ATTRIBUTE;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.TYPE_BUILDER;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.arrayOf;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;

import org.mule.runtime.api.MuleVersion;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.BinaryType;
import org.mule.metadata.api.model.BooleanType;
import org.mule.metadata.api.model.NullType;
import org.mule.metadata.api.model.NumberType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.Interceptable;
import org.mule.runtime.extension.api.introspection.config.ConfigurationFactory;
import org.mule.runtime.extension.api.introspection.config.ConfigurationModel;
import org.mule.runtime.extension.api.introspection.config.RuntimeConfigurationModel;
import org.mule.runtime.extension.api.introspection.connection.RuntimeConnectionProviderModel;
import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.spi.ModelEnricher;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.extension.api.introspection.operation.RuntimeOperationModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.extension.api.introspection.source.RuntimeSourceModel;
import org.mule.runtime.extension.tck.introspection.TestWebServiceConsumerDeclarer;
import org.mule.runtime.module.extension.internal.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.module.extension.internal.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.tck.size.SmallTest;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;

@SmallTest
public class FlatExtensionDeclarationTestCase extends BaseExtensionDeclarationTestCase {

  private static final MuleVersion MIN_MULE_VERSION = new MuleVersion("4.0");
  private final TestWebServiceConsumerDeclarer reference = new TestWebServiceConsumerDeclarer();

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void assertExtension() {
    assertThat(extensionModel.getName(), equalTo(WS_CONSUMER));
    assertThat(extensionModel.getDescription(), equalTo(WS_CONSUMER_DESCRIPTION));
    assertThat(extensionModel.getVersion(), equalTo(VERSION));
    assertThat(extensionModel.getConfigurationModels(), hasSize(1));
    assertThat(extensionModel.getVendor(), equalTo(MULESOFT));

    verify(serviceRegistry).lookupProviders(any(Class.class), any(ClassLoader.class));
  }

  @Test
  public void defaultConfiguration() throws Exception {
    RuntimeConfigurationModel configurationModel =
        (RuntimeConfigurationModel) extensionModel.getConfigurationModel(CONFIG_NAME).get();
    assertThat(configurationModel, is(notNullValue()));
    assertThat(configurationModel.getName(), equalTo(CONFIG_NAME));
    assertThat(configurationModel.getDescription(), equalTo(CONFIG_DESCRIPTION));
    assertThat(configurationModel.getExtensionModel(), is(sameInstance(extensionModel)));

    List<ParameterModel> parameterModels = configurationModel.getParameterModels();
    assertThat(parameterModels, hasSize(4));
    assertParameter(parameterModels.get(0), ADDRESS, SERVICE_ADDRESS, SUPPORTED, true, toMetadataType(String.class),
                    StringType.class, null);
    assertParameter(parameterModels.get(1), PORT, SERVICE_PORT, SUPPORTED, true, toMetadataType(String.class), StringType.class,
                    null);
    assertParameter(parameterModels.get(2), SERVICE, SERVICE_NAME, SUPPORTED, true, toMetadataType(String.class),
                    StringType.class, null);
    assertParameter(parameterModels.get(3), WSDL_LOCATION, URI_TO_FIND_THE_WSDL, NOT_SUPPORTED, true,
                    toMetadataType(String.class), StringType.class, null);
  }

  @Test
  public void onlyOneConfig() throws Exception {
    assertThat(extensionModel.getConfigurationModels(), hasSize(1));
    assertThat(extensionModel.getConfigurationModels().get(0),
               is(sameInstance(extensionModel.getConfigurationModel(CONFIG_NAME).get())));
  }

  public void noSuchConfiguration() throws Exception {
    assertThat(extensionModel.getConfigurationModel("fake").isPresent(), is(false));
  }

  public void noSuchOperation() throws Exception {
    assertThat(extensionModel.getOperationModel("fake").isPresent(), is(false));
  }

  @Test
  public void operations() throws Exception {
    List<OperationModel> operationModels = extensionModel.getOperationModels();
    assertThat(operationModels, hasSize(3));
    assertConsumeOperation(operationModels);
    assertBroadcastOperation(operationModels);
    assertArglessOperation(operationModels);
  }

  @Test(expected = IllegalArgumentException.class)
  public void badExtensionVersion() {
    factory.createFrom(new ExtensionDeclarer().named("bad").onVersion("i'm new"), createDescribingContext());
  }

  @Test
  public void configurationsOrder() {
    ConfigurationFactory mockInstantiator = mock(ConfigurationFactory.class);

    final String defaultConfiguration = "default";
    final String beta = "beta";
    final String alpha = "alpha";

    ExtensionDeclarer extensionDeclarer = new ExtensionDeclarer().named("test").onVersion("1.0").fromVendor("MuleSoft")
        .withCategory(COMMUNITY).withMinMuleVersion(MIN_MULE_VERSION);

    extensionDeclarer.withConfig(defaultConfiguration).describedAs(defaultConfiguration).createdWith(mockInstantiator);
    extensionDeclarer.withConfig(beta).describedAs(beta).createdWith(mockInstantiator);
    extensionDeclarer.withConfig(alpha).describedAs(alpha).createdWith(mockInstantiator);

    ExtensionModel extensionModel = factory.createFrom(extensionDeclarer, createDescribingContext());
    List<ConfigurationModel> configurationModels = extensionModel.getConfigurationModels();
    assertThat(configurationModels, hasSize(3));
    assertThat(configurationModels.get(1).getName(), equalTo(alpha));
    assertThat(configurationModels.get(2).getName(), equalTo(beta));
  }

  @Test
  public void operationsAlphaSorted() {
    assertThat(extensionModel.getOperationModels(), hasSize(3));
    assertThat(extensionModel.getOperationModels().get(0).getName(), equalTo(ARG_LESS));
    assertThat(extensionModel.getOperationModels().get(1).getName(), equalTo(BROADCAST));
    assertThat(extensionModel.getOperationModels().get(2).getName(), equalTo(CONSUMER));
  }

  @Test(expected = IllegalArgumentException.class)
  public void nameClashes() {
    extensionDeclarer.withConfig(CONFIG_NAME).createdWith(mock(ConfigurationFactory.class)).describedAs("");
    factory.createFrom(extensionDeclarer, createDescribingContext());
  }

  @Test(expected = IllegalParameterModelDefinitionException.class)
  public void operationWithParameterNamedName() {
    extensionDeclarer.withOperation("invalidOperation").describedAs("").withRequiredParameter("name")
        .ofType(toMetadataType(String.class));
    factory.createFrom(extensionDeclarer, createDescribingContext());
  }

  @Test(expected = IllegalParameterModelDefinitionException.class)
  public void fixedParameterWithExpressionDefault() {
    extensionDeclarer.withOperation("invalidOperation").describedAs("").withOptionalParameter("fixed")
        .ofType(toMetadataType(String.class)).withExpressionSupport(NOT_SUPPORTED).defaultingTo("#['hello']");

    factory.createFrom(extensionDeclarer, createDescribingContext());
  }

  @Test(expected = IllegalOperationModelDefinitionException.class)
  public void operationWithParameterNamedTarget() {
    extensionDeclarer.withOperation("invalidOperation").describedAs("").withOptionalParameter(TARGET_ATTRIBUTE)
        .ofType(toMetadataType(String.class));

    factory.createFrom(extensionDeclarer, createDescribingContext());
  }

  @Test(expected = IllegalParameterModelDefinitionException.class)
  public void expressionParameterWithFixedValue() {
    extensionDeclarer.withOperation("invalidOperation").describedAs("").withOptionalParameter("expression")
        .ofType(toMetadataType(String.class)).withExpressionSupport(REQUIRED).defaultingTo("static");

    factory.createFrom(extensionDeclarer, createDescribingContext());
  }

  @Test
  public void nullVendor() {
    exception.expect(IllegalModelDefinitionException.class);
    exception.expectMessage("Extension Vendor cannot be null");

    final ExtensionDeclarer baseDeclarer = getBaseDeclarer();
    baseDeclarer.withCategory(COMMUNITY).withMinMuleVersion(MIN_MULE_VERSION);

    factory.createFrom(baseDeclarer, createDescribingContext());
  }

  @Test
  public void nullCategory() {
    exception.expect(IllegalModelDefinitionException.class);
    exception.expectMessage("Extension Category cannot be null");

    final ExtensionDeclarer baseDeclarer = getBaseDeclarer();
    baseDeclarer.fromVendor("SomeVendor").withMinMuleVersion(MIN_MULE_VERSION);

    factory.createFrom(baseDeclarer, createDescribingContext());
  }

  @Test
  public void nullMinMuleVersion() {
    exception.expect(IllegalModelDefinitionException.class);
    exception.expectMessage("Extension Minimum Mule Version cannot be null");

    final ExtensionDeclarer baseDeclarer = getBaseDeclarer();
    baseDeclarer.fromVendor("SomeVendor").withCategory(COMMUNITY);

    factory.createFrom(baseDeclarer, createDescribingContext());
  }

  @Test
  public void configlessDescriptor() {
    factory.createFrom(new ExtensionDeclarer().named("noConfigs").onVersion("1.0").fromVendor("MuleSoft").withCategory(COMMUNITY)
        .withMinMuleVersion(MIN_MULE_VERSION), createDescribingContext());
  }

  @Test
  public void enrichersInvoked() throws Exception {
    ModelEnricher modelEnricher1 = mock(ModelEnricher.class);
    ModelEnricher modelEnricher2 = mock(ModelEnricher.class);

    when(serviceRegistry.lookupProviders(same(ModelEnricher.class), any(ClassLoader.class)))
        .thenReturn(Arrays.asList(modelEnricher1, modelEnricher2));

    factory = new DefaultExtensionFactory(serviceRegistry, getClass().getClassLoader());
    factory.createFrom(extensionDeclarer, createDescribingContext());

    assertDescribingContext(modelEnricher1);
    assertDescribingContext(modelEnricher2);
  }

  @Test
  public void executorsAreInterceptable() {
    for (OperationModel operation : extensionModel.getOperationModels()) {
      assertThat(((RuntimeOperationModel) operation).getExecutor().createExecutor(operation),
                 is(instanceOf(Interceptable.class)));
    }
  }

  @Test
  public void connectionProviders() {
    assertThat(extensionModel.getConnectionProviders(), hasSize(1));
    RuntimeConnectionProviderModel connectionProvider =
        (RuntimeConnectionProviderModel) extensionModel.getConnectionProviders().get(0);
    assertThat(connectionProvider, is(notNullValue()));
    assertThat(connectionProvider.getName(), is(CONNECTION_PROVIDER_NAME));
    assertThat(connectionProvider.getDescription(), is(CONNECTION_PROVIDER_DESCRIPTION));
    assertThat(connectionProvider.getConnectionProviderFactory(), is(sameInstance(reference.getConnectionProviderFactory())));
    assertThat(connectionProvider.getConnectionType(), is(sameInstance(CONNECTION_PROVIDER_CONNECTOR_TYPE)));

    List<ParameterModel> parameters = connectionProvider.getParameterModels();
    assertParameter(parameters.get(0), USERNAME, USERNAME_DESCRIPTION, SUPPORTED, true, toMetadataType(String.class),
                    StringType.class, null);
    assertParameter(parameters.get(1), PASSWORD, PASSWORD_DESCRIPTION, SUPPORTED, true, toMetadataType(String.class),
                    StringType.class, null);
  }

  @Test
  public void messageSources() {
    assertThat(extensionModel.getSourceModels(), hasSize(1));
    RuntimeSourceModel sourceModel = (RuntimeSourceModel) extensionModel.getSourceModels().get(0);
    assertThat(sourceModel, is(notNullValue()));
    assertThat(sourceModel.getName(), is(LISTENER));
    assertThat(sourceModel.getDescription(), is(LISTEN_DESCRIPTION));
    assertThat(sourceModel.getSourceFactory().createSource(), is(sameInstance(reference.getSource())));
    assertThat(getType(sourceModel.getOutput().getType()), is(equalTo(InputStream.class)));
    assertThat(getType(sourceModel.getOutputAttributes().getType()), is(equalTo(Serializable.class)));

    List<ParameterModel> parameters = sourceModel.getParameterModels();
    assertParameter(parameters.get(0), URL, URL_DESCRIPTION, SUPPORTED, true, toMetadataType(String.class), StringType.class,
                    null);
    assertParameter(parameters.get(1), PORT, PORT_DESCRIPTION, SUPPORTED, false, toMetadataType(Integer.class), NumberType.class,
                    DEFAULT_PORT);
  }

  private void assertDescribingContext(ModelEnricher modelEnricher) {
    ArgumentCaptor<DescribingContext> captor = ArgumentCaptor.forClass(DescribingContext.class);
    verify(modelEnricher).enrich(captor.capture());

    DescribingContext ctx = captor.getValue();
    assertThat(ctx, is(notNullValue()));
    assertThat(ctx.getExtensionDeclarer(), is(sameInstance(extensionDeclarer)));
  }

  private void assertConsumeOperation(List<OperationModel> operationModels) {
    OperationModel operationModel = operationModels.get(2);
    assertThat(operationModel, is(sameInstance(extensionModel.getOperationModel(CONSUMER).get())));
    assertDataType(operationModel.getOutput().getType(), InputStream.class, BinaryType.class);

    assertThat(operationModel.getName(), equalTo(CONSUMER));
    assertThat(operationModel.getDescription(), equalTo(GO_GET_THEM_TIGER));

    List<ParameterModel> parameterModels = operationModel.getParameterModels();
    assertThat(parameterModels, hasSize(2));
    assertParameter(parameterModels.get(0), OPERATION, THE_OPERATION_TO_USE, SUPPORTED, true, toMetadataType(String.class),
                    StringType.class, null);
    assertParameter(parameterModels.get(1), MTOM_ENABLED, MTOM_DESCRIPTION, SUPPORTED, false, toMetadataType(Boolean.class),
                    BooleanType.class, true);
  }

  private void assertBroadcastOperation(List<OperationModel> operationModels) {
    OperationModel operationModel = operationModels.get(1);
    assertThat(operationModel, is(sameInstance(extensionModel.getOperationModel(BROADCAST).get())));
    assertDataType(operationModel.getOutput().getType(), void.class, NullType.class);

    assertThat(operationModel.getName(), equalTo(BROADCAST));
    assertThat(operationModel.getDescription(), equalTo(BROADCAST_DESCRIPTION));

    List<ParameterModel> parameterModels = operationModel.getParameterModels();
    assertThat(parameterModels, hasSize(3));
    assertParameter(parameterModels.get(0), OPERATION, THE_OPERATION_TO_USE, SUPPORTED, true,
                    arrayOf(List.class, TYPE_BUILDER.stringType().id(String.class.getName())), ArrayType.class, null);
    assertParameter(parameterModels.get(1), MTOM_ENABLED, MTOM_DESCRIPTION, SUPPORTED, false, toMetadataType(Boolean.class),
                    BooleanType.class, true);
    assertParameter(parameterModels.get(2), CALLBACK, CALLBACK_DESCRIPTION, REQUIRED, true, toMetadataType(OperationModel.class),
                    ObjectType.class, null);
  }

  private void assertArglessOperation(List<OperationModel> operationModels) {
    OperationModel operationModel = operationModels.get(0);
    assertThat(operationModel, is(sameInstance(extensionModel.getOperationModel(ARG_LESS).get())));
    assertDataType(operationModel.getOutput().getType(), int.class, NumberType.class);

    assertThat(operationModel.getName(), equalTo(ARG_LESS));
    assertThat(operationModel.getDescription(), equalTo(HAS_NO_ARGS));

    List<ParameterModel> parameterModels = operationModel.getParameterModels();
    assertThat(parameterModels.isEmpty(), is(true));
  }

  private ExtensionDeclarer getBaseDeclarer() {
    final ExtensionDeclarer extensionDeclarer = new ExtensionDeclarer();
    extensionDeclarer.named("BaseExtension").onVersion("1.2.3").withConfig("default")
        .createdWith(mock(ConfigurationFactory.class));
    return extensionDeclarer;
  }

  @Override
  protected ExtensionDeclarer createDeclarationDescriptor() {
    return reference.getExtensionDeclarer();
  }
}
