/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.api.extension.introspection.DataQualifier.BOOLEAN;
import static org.mule.api.extension.introspection.DataQualifier.LIST;
import static org.mule.api.extension.introspection.DataQualifier.STRING;
import static org.mule.api.extension.introspection.DataType.of;
import static org.mule.extension.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.ADDRESS;
import static org.mule.extension.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.ARG_LESS;
import static org.mule.extension.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.BROADCAST;
import static org.mule.extension.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.BROADCAST_DESCRIPTION;
import static org.mule.extension.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.CALLBACK;
import static org.mule.extension.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.CALLBACK_DESCRIPTION;
import static org.mule.extension.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.CONFIG_DESCRIPTION;
import static org.mule.extension.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.CONFIG_NAME;
import static org.mule.extension.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.CONSUMER;
import static org.mule.extension.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.GO_GET_THEM_TIGER;
import static org.mule.extension.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.HAS_NO_ARGS;
import static org.mule.extension.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.MTOM_DESCRIPTION;
import static org.mule.extension.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.MTOM_ENABLED;
import static org.mule.extension.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.OPERATION;
import static org.mule.extension.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.PORT;
import static org.mule.extension.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.SERVICE;
import static org.mule.extension.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.SERVICE_ADDRESS;
import static org.mule.extension.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.SERVICE_NAME;
import static org.mule.extension.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.SERVICE_PORT;
import static org.mule.extension.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.THE_OPERATION_TO_USE;
import static org.mule.extension.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.URI_TO_FIND_THE_WSDL;
import static org.mule.extension.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.VERSION;
import static org.mule.extension.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.WSDL_LOCATION;
import static org.mule.extension.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.WS_CONSUMER;
import static org.mule.extension.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.WS_CONSUMER_DESCRIPTION;
import org.mule.api.registry.ServiceRegistry;
import org.mule.api.extension.exception.NoSuchConfigurationException;
import org.mule.api.extension.exception.NoSuchOperationException;
import org.mule.api.extension.introspection.ConfigurationInstantiator;
import org.mule.api.extension.introspection.ConfigurationModel;
import org.mule.api.extension.introspection.DataQualifier;
import org.mule.api.extension.introspection.DataType;
import org.mule.api.extension.introspection.ExtensionFactory;
import org.mule.api.extension.introspection.ExtensionModel;
import org.mule.api.extension.introspection.OperationModel;
import org.mule.api.extension.introspection.ParameterModel;
import org.mule.api.extension.introspection.declaration.DescribingContext;
import org.mule.api.extension.introspection.declaration.fluent.DeclarationDescriptor;
import org.mule.api.extension.introspection.declaration.spi.ModelEnricher;
import org.mule.extension.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference;
import org.mule.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ExtensionBuildersTestCase extends AbstractMuleTestCase
{

    @Mock
    private ServiceRegistry serviceRegistry;
    private DeclarationDescriptor descriptor;
    private ExtensionModel extensionModel;

    private ExtensionFactory factory;

    @Before
    public void buildExtension() throws Exception
    {
        Collection<ModelEnricher> emptyList = Collections.emptyList();
        when(serviceRegistry.lookupProviders(same(ModelEnricher.class))).thenReturn(emptyList);
        when(serviceRegistry.lookupProviders(same(ModelEnricher.class), any(ClassLoader.class))).thenReturn(emptyList);

        factory = new DefaultExtensionFactory(serviceRegistry, getClass().getClassLoader());
        descriptor = createDeclarationDescriptor();
        extensionModel = factory.createFrom(descriptor);
    }

    @Test
    public void assertExtension()
    {
        assertThat(extensionModel.getName(), equalTo(WS_CONSUMER));
        assertThat(extensionModel.getDescription(), equalTo(WS_CONSUMER_DESCRIPTION));
        assertThat(extensionModel.getVersion(), equalTo(VERSION));
        assertThat(extensionModel.getConfigurations(), hasSize(1));

        verify(serviceRegistry).lookupProviders(any(Class.class), any(ClassLoader.class));
    }

    @Test
    public void defaultConfiguration() throws Exception
    {
        ConfigurationModel configurationModel = extensionModel.getConfiguration(CONFIG_NAME);
        assertThat(configurationModel, is(notNullValue()));
        assertThat(configurationModel.getName(), equalTo(CONFIG_NAME));
        assertThat(configurationModel.getDescription(), equalTo(CONFIG_DESCRIPTION));
        assertThat(configurationModel.getExtensionModel(), is(sameInstance(extensionModel)));

        List<ParameterModel> parameterModels = configurationModel.getParameterModels();
        assertThat(parameterModels, hasSize(4));
        assertParameter(parameterModels.get(0), ADDRESS, SERVICE_ADDRESS, true, true, of(String.class), STRING, null);
        assertParameter(parameterModels.get(1), PORT, SERVICE_PORT, true, true, of(String.class), STRING, null);
        assertParameter(parameterModels.get(2), SERVICE, SERVICE_NAME, true, true, of(String.class), STRING, null);
        assertParameter(parameterModels.get(3), WSDL_LOCATION, URI_TO_FIND_THE_WSDL, false, true, of(String.class), STRING, null);
    }

    @Test
    public void onlyOneConfig() throws Exception
    {
        assertThat(extensionModel.getConfigurations(), hasSize(1));
        assertThat(extensionModel.getConfigurations().get(0), is(sameInstance(extensionModel.getConfiguration(CONFIG_NAME))));
    }

    @Test(expected = NoSuchConfigurationException.class)
    public void noSuchConfiguration() throws Exception
    {
        extensionModel.getConfiguration("fake");
    }

    @Test(expected = NoSuchOperationException.class)
    public void noSuchOperation() throws Exception
    {
        extensionModel.getOperation("fake");
    }

    @Test
    public void operations() throws Exception
    {
        List<OperationModel> operationModels = extensionModel.getOperations();
        assertThat(operationModels, hasSize(3));
        assertConsumeOperation(operationModels);
        assertBroadcastOperation(operationModels);
        assertArglessOperation(operationModels);
    }

    @Test(expected = IllegalArgumentException.class)
    public void badExtensionVersion()
    {
        factory.createFrom(new DeclarationDescriptor().named("bad").onVersion("i'm new"));
    }

    @Test
    public void configurationsOrder()
    {
        ConfigurationInstantiator mockInstantiator = mock(ConfigurationInstantiator.class);

        final String defaultConfiguration = "default";
        final String beta = "beta";
        final String alpha = "alpha";

        ExtensionModel extensionModel = factory.createFrom(new DeclarationDescriptor().named("test").onVersion("1.0")
                                                                   .withConfig(defaultConfiguration).describedAs(defaultConfiguration).instantiatedWith(mockInstantiator)
                                                                   .withConfig(beta).describedAs(beta).instantiatedWith(mockInstantiator)
                                                                   .withConfig(alpha).describedAs(alpha).instantiatedWith(mockInstantiator));

        List<ConfigurationModel> configurationModels = extensionModel.getConfigurations();
        assertThat(configurationModels, hasSize(3));
        assertThat(configurationModels.get(1).getName(), equalTo(alpha));
        assertThat(configurationModels.get(2).getName(), equalTo(beta));
    }

    @Test
    public void operationsAlphaSorted()
    {
        assertThat(extensionModel.getOperations(), hasSize(3));
        assertThat(extensionModel.getOperations().get(0).getName(), equalTo(ARG_LESS));
        assertThat(extensionModel.getOperations().get(1).getName(), equalTo(BROADCAST));
        assertThat(extensionModel.getOperations().get(2).getName(), equalTo(CONSUMER));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nameClashes()
    {
        factory.createFrom(descriptor.withConfig(CONFIG_NAME).describedAs(""));
    }

    @Test(expected = IllegalArgumentException.class)
    public void operationWithParameterNamedName()
    {
        factory.createFrom(descriptor.withOperation("invalidOperation").describedAs("")
                                   .with().requiredParameter("name").ofType(String.class));
    }


    @Test(expected = IllegalArgumentException.class)
    public void nameWithSpaces()
    {
        descriptor = new DeclarationDescriptor().named("i have spaces").onVersion("1.0").withConfig("default").getRootDeclaration();
        factory.createFrom(descriptor);
    }

    @Test(expected = IllegalArgumentException.class)
    public void configlessDescriptor()
    {

        factory.createFrom(new DeclarationDescriptor().named("noConfigs").onVersion("1.0"));
    }

    @Test
    public void enrichersInvoked() throws Exception
    {
        ModelEnricher modelEnricher1 = mock(ModelEnricher.class);
        ModelEnricher modelEnricher2 = mock(ModelEnricher.class);

        when(serviceRegistry.lookupProviders(same(ModelEnricher.class), any(ClassLoader.class)))
                .thenReturn(Arrays.asList(modelEnricher1, modelEnricher2));

        factory = new DefaultExtensionFactory(serviceRegistry, getClass().getClassLoader());
        factory.createFrom(descriptor);

        assertDescribingContext(modelEnricher1);
        assertDescribingContext(modelEnricher2);
    }

    private void assertDescribingContext(ModelEnricher modelEnricher)
    {
        ArgumentCaptor<DescribingContext> captor = ArgumentCaptor.forClass(DescribingContext.class);
        verify(modelEnricher).enrich(captor.capture());

        DescribingContext ctx = captor.getValue();
        assertThat(ctx, is(notNullValue()));
        assertThat(ctx.getDeclarationDescriptor(), is(sameInstance(descriptor)));
    }

    private void assertConsumeOperation(List<OperationModel> operationModels) throws NoSuchOperationException
    {
        OperationModel operationModel = operationModels.get(2);
        assertThat(operationModel, is(sameInstance(extensionModel.getOperation(CONSUMER))));

        assertThat(operationModel.getName(), equalTo(CONSUMER));
        assertThat(operationModel.getDescription(), equalTo(GO_GET_THEM_TIGER));

        List<ParameterModel> parameterModels = operationModel.getParameterModels();
        assertThat(parameterModels, hasSize(2));
        assertParameter(parameterModels.get(0), OPERATION, THE_OPERATION_TO_USE, true, true, of(String.class), STRING, null);
        assertParameter(parameterModels.get(1), MTOM_ENABLED, MTOM_DESCRIPTION, true, false, of(Boolean.class), BOOLEAN, true);
    }

    private void assertBroadcastOperation(List<OperationModel> operationModels) throws NoSuchOperationException
    {
        OperationModel operationModel = operationModels.get(1);
        assertThat(operationModel, is(sameInstance(extensionModel.getOperation(BROADCAST))));

        assertThat(operationModel.getName(), equalTo(BROADCAST));
        assertThat(operationModel.getDescription(), equalTo(BROADCAST_DESCRIPTION));

        List<ParameterModel> parameterModels = operationModel.getParameterModels();
        assertThat(parameterModels, hasSize(3));
        assertParameter(parameterModels.get(0), OPERATION, THE_OPERATION_TO_USE, true, true, of(List.class, String.class), LIST, null);
        assertParameter(parameterModels.get(1), MTOM_ENABLED, MTOM_DESCRIPTION, true, false, of(Boolean.class), BOOLEAN, true);
        assertParameter(parameterModels.get(2), CALLBACK, CALLBACK_DESCRIPTION, false, true, of(OperationModel.class), DataQualifier.OPERATION, null);
    }

    private void assertArglessOperation(List<OperationModel> operationModels) throws NoSuchOperationException
    {
        OperationModel operationModel = operationModels.get(0);
        assertThat(operationModel, is(sameInstance(extensionModel.getOperation(ARG_LESS))));

        assertThat(operationModel.getName(), equalTo(ARG_LESS));
        assertThat(operationModel.getDescription(), equalTo(HAS_NO_ARGS));

        List<ParameterModel> parameterModels = operationModel.getParameterModels();
        assertThat(parameterModels.isEmpty(), is(true));
    }


    private void assertParameter(ParameterModel parameterModel,
                                 String name,
                                 String description,
                                 boolean acceptsExpressions,
                                 boolean required,
                                 DataType type,
                                 DataQualifier qualifier,
                                 Object defaultValue)
    {
        assertThat(parameterModel, is(notNullValue()));
        assertThat(parameterModel.getName(), equalTo(name));
        assertThat(parameterModel.getDescription(), equalTo(description));
        assertThat(parameterModel.isDynamic(), is(acceptsExpressions));
        assertThat(parameterModel.isRequired(), is(required));
        assertThat(parameterModel.getType(), equalTo(type));
        assertThat(parameterModel.getType().getQualifier(), is(qualifier));

        if (defaultValue != null)
        {
            assertThat(parameterModel.getDefaultValue(), equalTo(defaultValue));
        }
        else
        {
            assertThat(parameterModel.getDefaultValue(), is(nullValue()));
        }
    }

    private DeclarationDescriptor createDeclarationDescriptor()
    {
        return new TestWebServiceConsumerDeclarationReference().getDescriptor();
    }
}
