/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.api.meta.model.tck.TestHttpConnectorDeclarer.EXTENSION_DESCRIPTION;
import static org.mule.runtime.api.meta.model.tck.TestHttpConnectorDeclarer.EXTENSION_NAME;
import static org.mule.runtime.api.meta.model.tck.TestHttpConnectorDeclarer.LISTENER_CONFIG_DESCRIPTION;
import static org.mule.runtime.api.meta.model.tck.TestHttpConnectorDeclarer.LISTENER_CONFIG_NAME;
import static org.mule.runtime.api.meta.model.tck.TestHttpConnectorDeclarer.LISTEN_MESSAGE_SOURCE;
import static org.mule.runtime.api.meta.model.tck.TestHttpConnectorDeclarer.PATH;
import static org.mule.runtime.api.meta.model.tck.TestHttpConnectorDeclarer.PORT;
import static org.mule.runtime.api.meta.model.tck.TestHttpConnectorDeclarer.REQUESTER_CONFIG_DESCRIPTION;
import static org.mule.runtime.api.meta.model.tck.TestHttpConnectorDeclarer.REQUESTER_CONFIG_NAME;
import static org.mule.runtime.api.meta.model.tck.TestHttpConnectorDeclarer.REQUESTER_PROVIDER;
import static org.mule.runtime.api.meta.model.tck.TestHttpConnectorDeclarer.REQUEST_OPERATION_NAME;
import static org.mule.runtime.api.meta.model.tck.TestHttpConnectorDeclarer.STATIC_RESOURCE_OPERATION_NAME;
import static org.mule.runtime.api.meta.model.tck.TestHttpConnectorDeclarer.VENDOR;
import static org.mule.runtime.api.meta.model.tck.TestHttpConnectorDeclarer.VERSION;
import static org.mule.runtime.extension.api.ExtensionConstants.RECONNECTION_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.REDELIVERY_POLICY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.STREAMING_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_PARAMETER_NAME;
import static org.mule.runtime.extension.internal.loader.enricher.MimeTypeParametersDeclarationEnricher.ENCODING_PARAMETER_NAME;
import static org.mule.runtime.extension.internal.loader.enricher.MimeTypeParametersDeclarationEnricher.MIME_TYPE_PARAMETER_NAME;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.assertType;

import org.mule.metadata.api.model.BinaryType;
import org.mule.metadata.api.model.NumberType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.model.UnionType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.tck.TestHttpConnectorDeclarer;
import org.mule.runtime.extension.api.declaration.type.ReconnectionStrategyTypeBuilder;
import org.mule.runtime.extension.api.declaration.type.RedeliveryPolicyTypeBuilder;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionDeclarationTestCase;
import org.mule.tck.size.SmallTest;
import org.junit.Before;
import org.junit.Test;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

@SmallTest
public class ComplexJavaExtensionDeclarationTestCase extends AbstractJavaExtensionDeclarationTestCase {

  private ExtensionModel extensionModel;

  @Before
  public void before() {
    extensionModel = new ExtensionModelLoader() {

      @Override
      public String getId() {
        return "test";
      }

      @Override
      protected void declareExtension(ExtensionLoadingContext context) {
        new TestHttpConnectorDeclarer().declareOn(context.getExtensionDeclarer());
      }
    }.loadExtensionModel(getClass().getClassLoader(), getDefault(emptySet()), new HashMap<>());
  }


  @Test
  public void assertDeclaration() {
    assertThat(extensionModel.getName(), is(EXTENSION_NAME));
    assertThat(extensionModel.getDescription(), is(EXTENSION_DESCRIPTION));
    assertThat(extensionModel.getVersion(), is(VERSION));
    assertThat(extensionModel.getConfigurationModels(), hasSize(2));
    assertThat(extensionModel.getVendor(), is(VENDOR));
    assertThat(extensionModel.getOperationModels(), hasSize(1));
    assertThat(extensionModel.getConnectionProviders(), is(empty()));
    assertThat(extensionModel.getSourceModels(), is(empty()));
  }

  @Test
  public void listenerConfig() {
    ConfigurationModel listener = extensionModel.getConfigurationModel(LISTENER_CONFIG_NAME).get();
    assertThat(listener.getDescription(), is(LISTENER_CONFIG_DESCRIPTION));
    assertThat(listener.getOperationModels(), is(empty()));
    assertThat(listener.getConnectionProviders(), is(empty()));
    assertThat(listener.getSourceModels(), hasSize(1));
  }

  @Test
  public void listenerSource() {
    SourceModel source =
        extensionModel.getConfigurationModel(LISTENER_CONFIG_NAME).get().getSourceModel(LISTEN_MESSAGE_SOURCE).get();
    assertType(source.getOutput().getType(), InputStream.class, BinaryType.class);
    assertType(source.getOutputAttributes().getType(), Serializable.class, ObjectType.class);

    List<ParameterModel> parameters = source.getAllParameterModels();
    assertThat(parameters, hasSize(6));

    assertMimeTypeParams(parameters);

    ParameterModel parameter = parameters.get(2);
    assertThat(parameter.getName(), is(REDELIVERY_POLICY_PARAMETER_NAME));
    assertThat(parameter.getType(), equalTo(new RedeliveryPolicyTypeBuilder().buildRedeliveryPolicyType()));

    parameter = parameters.get(3);
    assertStreamingStrategyParameter(parameter);

    parameter = parameters.get(4);
    assertThat(parameter.getName(), is(PORT));
    assertThat(parameter.isRequired(), is(false));
    assertType(parameter.getType(), Integer.class, NumberType.class);

    parameter = parameters.get(5);
    assertThat(parameter.getName(), is(RECONNECTION_STRATEGY_PARAMETER_NAME));
    assertThat(parameter.getType(), equalTo(new ReconnectionStrategyTypeBuilder().buildReconnectionStrategyType()));
  }

  @Test
  public void requesterConfig() {
    ConfigurationModel listener = extensionModel.getConfigurationModel(REQUESTER_CONFIG_NAME).get();
    assertThat(listener.getDescription(), is(REQUESTER_CONFIG_DESCRIPTION));
    assertThat(listener.getOperationModels(), hasSize(1));
    assertThat(listener.getConnectionProviders(), hasSize(1));
    assertThat(listener.getSourceModels(), is(empty()));
  }

  @Test
  public void requestOperation() {
    OperationModel operation =
        extensionModel.getConfigurationModel(REQUESTER_CONFIG_NAME).get().getOperationModel(REQUEST_OPERATION_NAME).get();
    assertThat(operation.getName(), is(REQUEST_OPERATION_NAME));
    assertType(operation.getOutput().getType(), InputStream.class, BinaryType.class);
    List<ParameterModel> parameterModels = operation.getAllParameterModels();
    assertThat(parameterModels, hasSize(5));

    assertMimeTypeParams(parameterModels);

    ParameterModel parameter = parameterModels.get(2);
    assertStreamingStrategyParameter(parameter);

    parameter = parameterModels.get(3);
    assertThat(parameter.getName(), is(PATH));
    assertType(parameter.getType(), String.class, StringType.class);

    parameter = parameterModels.get(4);
    assertTargetParameter(parameter);
  }

  private void assertTargetParameter(ParameterModel parameter) {
    assertThat(parameter.getName(), is(TARGET_PARAMETER_NAME));
    assertType(parameter.getType(), String.class, StringType.class);
  }

  private void assertStreamingStrategyParameter(ParameterModel parameter) {
    assertThat(parameter.getName(), is(STREAMING_STRATEGY_PARAMETER_NAME));
    assertType(parameter.getType(), Object.class, UnionType.class);
  }

  private void assertMimeTypeParams(List<ParameterModel> parameters) {
    assertThat(parameters.get(0).getName(), is(MIME_TYPE_PARAMETER_NAME));
    assertThat(parameters.get(1).getName(), is(ENCODING_PARAMETER_NAME));
  }

  @Test
  public void staticResourceOperation() {
    OperationModel operation = extensionModel.getOperationModel(STATIC_RESOURCE_OPERATION_NAME).get();
    assertThat(operation.getName(), is(STATIC_RESOURCE_OPERATION_NAME));
    assertType(operation.getOutput().getType(), InputStream.class, BinaryType.class);
    final List<ParameterModel> parameters = operation.getAllParameterModels();
    assertThat(parameters, hasSize(5));

    assertMimeTypeParams(parameters);
    assertStreamingStrategyParameter(parameters.get(2));
    ParameterModel parameter = parameters.get(3);
    assertThat(parameter.getName(), is(PATH));
    assertType(parameter.getType(), String.class, StringType.class);

    assertTargetParameter(parameters.get(4));
  }

  @Test
  public void connectionProvider() {
    ConnectionProviderModel provider =
        extensionModel.getConfigurationModel(REQUESTER_CONFIG_NAME).get().getConnectionProviders().get(0);
    assertThat(provider.getName(), is(REQUESTER_PROVIDER));
  }
}
