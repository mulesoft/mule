/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
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
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_PARAMETER_NAME;
import org.mule.metadata.api.model.BinaryType;
import org.mule.metadata.api.model.NumberType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.tck.TestHttpConnectorDeclarer;
import org.mule.runtime.extension.api.declaration.type.RedeliveryPolicyTypeBuilder;
import org.mule.runtime.extension.api.declaration.type.ReconnectionStrategyTypeBuilder;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionDeclarationTestCase;
import org.mule.tck.size.SmallTest;

import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class ComplexExtensionDeclarationTestCase extends AbstractJavaExtensionDeclarationTestCase {

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
    }.loadExtensionModel(getClass().getClassLoader(), new HashMap<>());
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
    assertDataType(source.getOutput().getType(), InputStream.class, BinaryType.class);
    assertDataType(source.getOutputAttributes().getType(), Serializable.class, ObjectType.class);

    List<ParameterModel> parameters = source.getAllParameterModels();
    assertThat(parameters, hasSize(3));

    ParameterModel parameter = parameters.get(0);
    assertThat(parameter.getName(), is(REDELIVERY_POLICY_PARAMETER_NAME));
    assertThat(parameter.getType(), equalTo(new RedeliveryPolicyTypeBuilder().buildRetryPolicyType()));

    parameter = parameters.get(1);
    assertThat(parameter.getName(), is(RECONNECTION_STRATEGY_PARAMETER_NAME));
    assertThat(parameter.getType(), equalTo(new ReconnectionStrategyTypeBuilder().builReconnectionStrategyType()));

    parameter = parameters.get(2);
    assertThat(parameter.getName(), is(PORT));
    assertThat(parameter.isRequired(), is(false));
    assertDataType(parameter.getType(), Integer.class, NumberType.class);
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
    assertDataType(operation.getOutput().getType(), InputStream.class, BinaryType.class);
    assertThat(operation.getAllParameterModels(), hasSize(2));

    ParameterModel parameter = operation.getAllParameterModels().get(0);
    assertTargetParameter(parameter);

    parameter = operation.getAllParameterModels().get(1);
    assertThat(parameter.getName(), is(PATH));
    assertDataType(parameter.getType(), String.class, StringType.class);
  }

  private void assertTargetParameter(ParameterModel parameter) {
    assertThat(parameter.getName(), is(TARGET_PARAMETER_NAME));
    assertDataType(parameter.getType(), String.class, StringType.class);
  }

  @Test
  public void staticResourceOperation() {
    OperationModel operation = extensionModel.getOperationModel(STATIC_RESOURCE_OPERATION_NAME).get();
    assertThat(operation.getName(), is(STATIC_RESOURCE_OPERATION_NAME));
    assertDataType(operation.getOutput().getType(), InputStream.class, BinaryType.class);
    final List<ParameterModel> parameters = operation.getAllParameterModels();
    assertThat(parameters, hasSize(2));

    assertTargetParameter(parameters.get(0));

    ParameterModel parameter = parameters.get(1);
    assertThat(parameter.getName(), is(PATH));
    assertDataType(parameter.getType(), String.class, StringType.class);
  }

  @Test
  public void connectionProvider() {
    ConnectionProviderModel provider =
        extensionModel.getConfigurationModel(REQUESTER_CONFIG_NAME).get().getConnectionProviders().get(0);
    assertThat(provider.getName(), is(REQUESTER_PROVIDER));
  }
}
