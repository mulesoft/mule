/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.extension.api.introspection.declaration.tck.TestHttpConnectorDeclarer.EXTENSION_DESCRIPTION;
import static org.mule.extension.api.introspection.declaration.tck.TestHttpConnectorDeclarer.EXTENSION_NAME;
import static org.mule.extension.api.introspection.declaration.tck.TestHttpConnectorDeclarer.LISTENER_CONFIG_DESCRIPTION;
import static org.mule.extension.api.introspection.declaration.tck.TestHttpConnectorDeclarer.LISTENER_CONFIG_NAME;
import static org.mule.extension.api.introspection.declaration.tck.TestHttpConnectorDeclarer.LISTEN_MESSAGE_SOURCE;
import static org.mule.extension.api.introspection.declaration.tck.TestHttpConnectorDeclarer.PATH;
import static org.mule.extension.api.introspection.declaration.tck.TestHttpConnectorDeclarer.PORT;
import static org.mule.extension.api.introspection.declaration.tck.TestHttpConnectorDeclarer.REQUESTER_CONFIG_DESCRIPTION;
import static org.mule.extension.api.introspection.declaration.tck.TestHttpConnectorDeclarer.REQUESTER_CONFIG_NAME;
import static org.mule.extension.api.introspection.declaration.tck.TestHttpConnectorDeclarer.REQUESTER_CONNECTION_PROVIDER_CONFIG_TYPE;
import static org.mule.extension.api.introspection.declaration.tck.TestHttpConnectorDeclarer.REQUESTER_CONNECTION_PROVIDER_CONNECTION_TYPE;
import static org.mule.extension.api.introspection.declaration.tck.TestHttpConnectorDeclarer.REQUESTER_PROVIDER;
import static org.mule.extension.api.introspection.declaration.tck.TestHttpConnectorDeclarer.REQUEST_OPERATION_NAME;
import static org.mule.extension.api.introspection.declaration.tck.TestHttpConnectorDeclarer.STATIC_RESOURCE_OPERATION_NAME;
import static org.mule.extension.api.introspection.declaration.tck.TestHttpConnectorDeclarer.VENDOR;
import static org.mule.extension.api.introspection.declaration.tck.TestHttpConnectorDeclarer.VERSION;
import org.mule.extension.api.introspection.ConfigurationModel;
import org.mule.extension.api.introspection.ConnectionProviderModel;
import org.mule.extension.api.introspection.OperationModel;
import org.mule.extension.api.introspection.ParameterModel;
import org.mule.extension.api.introspection.SourceModel;
import org.mule.extension.api.introspection.declaration.fluent.ExtensionDeclarer;
import org.mule.extension.api.introspection.declaration.tck.TestHttpConnectorDeclarer;
import org.mule.metadata.api.model.BinaryType;
import org.mule.metadata.api.model.NumberType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.tck.size.SmallTest;

import java.io.InputStream;
import java.io.Serializable;

import org.junit.Test;


@SmallTest
public class ComplexExtensionDeclarationTestCase extends BaseExtensionDeclarationTestCase
{

    private final TestHttpConnectorDeclarer reference = new TestHttpConnectorDeclarer();

    @Test
    public void assertDeclaration()
    {
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
    public void listenerConfig()
    {
        ConfigurationModel listener = extensionModel.getConfigurationModel(LISTENER_CONFIG_NAME).get();
        assertThat(listener.getDescription(), is(LISTENER_CONFIG_DESCRIPTION));
        assertThat(listener.getOperationModels(), is(empty()));
        assertThat(listener.getConnectionProviders(), is(empty()));
        assertThat(listener.getSourceModels(), hasSize(1));
    }

    @Test
    public void listenerSource()
    {
        SourceModel source = extensionModel.getConfigurationModel(LISTENER_CONFIG_NAME).get().getSourceModel(LISTEN_MESSAGE_SOURCE).get();
        assertDataType(source.getReturnType(), InputStream.class, BinaryType.class);
        assertDataType(source.getAttributesType(), Serializable.class, ObjectType.class);
        assertThat(source.getParameterModels(), hasSize(1));

        ParameterModel parameter = source.getParameterModels().get(0);
        assertThat(parameter.getName(), is(PORT));
        assertThat(parameter.isRequired(), is(false));
        assertDataType(parameter.getType(), Integer.class, NumberType.class);
    }

    @Test
    public void requesterConfig()
    {
        ConfigurationModel listener = extensionModel.getConfigurationModel(REQUESTER_CONFIG_NAME).get();
        assertThat(listener.getDescription(), is(REQUESTER_CONFIG_DESCRIPTION));
        assertThat(listener.getOperationModels(), hasSize(1));
        assertThat(listener.getConnectionProviders(), hasSize(1));
        assertThat(listener.getSourceModels(), is(empty()));
    }

    @Test
    public void requestOperation()
    {
        OperationModel operation = extensionModel.getConfigurationModel(REQUESTER_CONFIG_NAME).get().getOperationModel(REQUEST_OPERATION_NAME).get();
        assertThat(operation.getName(), is(REQUEST_OPERATION_NAME));
        assertDataType(operation.getReturnType(), InputStream.class, BinaryType.class);
        assertThat(operation.getParameterModels(), hasSize(1));

        ParameterModel parameter = operation.getParameterModels().get(0);
        assertThat(parameter.getName(), is(PATH));
        assertDataType(parameter.getType(), String.class, StringType.class);
    }

    @Test
    public void staticResourceOperation()
    {
        OperationModel operation = extensionModel.getOperationModel(STATIC_RESOURCE_OPERATION_NAME).get();
        assertThat(operation.getName(), is(STATIC_RESOURCE_OPERATION_NAME));
        assertDataType(operation.getReturnType(), InputStream.class, BinaryType.class);
        assertThat(operation.getParameterModels(), hasSize(1));

        ParameterModel parameter = operation.getParameterModels().get(0);
        assertThat(parameter.getName(), is(PATH));
        assertDataType(parameter.getType(), String.class, StringType.class);
    }

    @Test
    public void connectionProvider()
    {
        ConnectionProviderModel provider = extensionModel.getConfigurationModel(REQUESTER_CONFIG_NAME).get().getConnectionProviders().get(0);
        assertThat(provider.getName(), is(REQUESTER_PROVIDER));
        assertThat(provider.getConfigurationType(), equalTo(REQUESTER_CONNECTION_PROVIDER_CONFIG_TYPE));
        assertThat(provider.getConnectionType(), equalTo(REQUESTER_CONNECTION_PROVIDER_CONNECTION_TYPE));
    }

    @Override
    protected ExtensionDeclarer createDeclarationDescriptor()
    {
        return reference.getExtensionDeclarer();
    }
}
