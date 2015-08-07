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
import static org.mule.extension.introspection.DataQualifier.BOOLEAN;
import static org.mule.extension.introspection.DataQualifier.LIST;
import static org.mule.extension.introspection.DataQualifier.STRING;
import static org.mule.extension.introspection.DataType.of;
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
import org.mule.extension.exception.NoSuchConfigurationException;
import org.mule.extension.exception.NoSuchOperationException;
import org.mule.extension.introspection.Configuration;
import org.mule.extension.introspection.ConfigurationInstantiator;
import org.mule.extension.introspection.DataQualifier;
import org.mule.extension.introspection.DataType;
import org.mule.extension.introspection.Extension;
import org.mule.extension.introspection.ExtensionFactory;
import org.mule.extension.introspection.Operation;
import org.mule.extension.introspection.Parameter;
import org.mule.extension.introspection.declaration.DescribingContext;
import org.mule.extension.introspection.declaration.fluent.DeclarationDescriptor;
import org.mule.extension.introspection.declaration.spi.DescriberPostProcessor;
import org.mule.extension.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference;
import org.mule.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
    private Extension extension;

    private ExtensionFactory factory;

    @Before
    public void buildExtension() throws Exception
    {
        Collection<DescriberPostProcessor> emptyList = Collections.emptyList();
        when(serviceRegistry.lookupProviders(same(DescriberPostProcessor.class))).thenReturn(emptyList);
        when(serviceRegistry.lookupProviders(same(DescriberPostProcessor.class), any(ClassLoader.class))).thenReturn(emptyList);

        factory = new DefaultExtensionFactory(serviceRegistry);
        descriptor = createDeclarationDescriptor();
        extension = factory.createFrom(descriptor);
    }

    @Test
    public void assertExtension()
    {
        assertThat(extension.getName(), equalTo(WS_CONSUMER));
        assertThat(extension.getDescription(), equalTo(WS_CONSUMER_DESCRIPTION));
        assertThat(extension.getVersion(), equalTo(VERSION));
        assertThat(extension.getConfigurations(), hasSize(1));

        Set<Object> capabilities = extension.getCapabilities(Object.class);
        assertThat(capabilities, is(notNullValue()));
        assertThat(capabilities, hasSize(1));

        verify(serviceRegistry).lookupProviders(any(Class.class), any(ClassLoader.class));
    }

    @Test
    public void defaultConfiguration() throws Exception
    {
        Configuration configuration = extension.getConfiguration(CONFIG_NAME);
        assertThat(configuration, is(notNullValue()));
        assertThat(configuration.getName(), equalTo(CONFIG_NAME));
        assertThat(configuration.getDescription(), equalTo(CONFIG_DESCRIPTION));

        List<Parameter> parameters = configuration.getParameters();
        assertThat(parameters, hasSize(4));
        assertParameter(parameters.get(0), ADDRESS, SERVICE_ADDRESS, true, true, of(String.class), STRING, null);
        assertParameter(parameters.get(1), PORT, SERVICE_PORT, true, true, of(String.class), STRING, null);
        assertParameter(parameters.get(2), SERVICE, SERVICE_NAME, true, true, of(String.class), STRING, null);
        assertParameter(parameters.get(3), WSDL_LOCATION, URI_TO_FIND_THE_WSDL, false, true, of(String.class), STRING, null);
    }

    @Test
    public void onlyOneConfig() throws Exception
    {
        assertThat(extension.getConfigurations(), hasSize(1));
        assertThat(extension.getConfigurations().get(0), is(sameInstance(extension.getConfiguration(CONFIG_NAME))));
    }

    @Test(expected = NoSuchConfigurationException.class)
    public void noSuchConfiguration() throws Exception
    {
        extension.getConfiguration("fake");
    }

    @Test(expected = NoSuchOperationException.class)
    public void noSuchOperation() throws Exception
    {
        extension.getOperation("fake");
    }

    @Test
    public void noSuchCapability()
    {
        Set<String> capabilities = extension.getCapabilities(String.class);
        assertThat(capabilities, is(notNullValue()));
        assertThat(capabilities, hasSize(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullCapability()
    {
        factory.createFrom(createDeclarationDescriptor().withCapability(null));
    }

    @Test
    public void operations() throws Exception
    {
        List<Operation> operations = extension.getOperations();
        assertThat(operations, hasSize(3));
        assertConsumeOperation(operations);
        assertBroadcastOperation(operations);
        assertArglessOperation(operations);
    }

    @Test(expected = IllegalArgumentException.class)
    public void badExtensionVersion()
    {
        factory.createFrom(new DeclarationDescriptor("bad", "i'm new"));
    }

    @Test
    public void configurationsOrder()
    {
        ConfigurationInstantiator mockInstantiator = mock(ConfigurationInstantiator.class);

        final String defaultConfiguration = "default";
        final String beta = "beta";
        final String alpha = "alpha";

        Extension extension = factory.createFrom(new DeclarationDescriptor("test", "1.0")
                                                         .withConfig(defaultConfiguration).describedAs(defaultConfiguration).instantiatedWith(mockInstantiator)
                                                         .withConfig(beta).describedAs(beta).instantiatedWith(mockInstantiator)
                                                         .withConfig(alpha).describedAs(alpha).instantiatedWith(mockInstantiator));

        List<Configuration> configurations = extension.getConfigurations();
        assertThat(configurations, hasSize(3));
        assertThat(configurations.get(1).getName(), equalTo(alpha));
        assertThat(configurations.get(2).getName(), equalTo(beta));
    }

    @Test
    public void operationsAlphaSorted()
    {
        assertThat(extension.getOperations(), hasSize(3));
        assertThat(extension.getOperations().get(0).getName(), equalTo(ARG_LESS));
        assertThat(extension.getOperations().get(1).getName(), equalTo(BROADCAST));
        assertThat(extension.getOperations().get(2).getName(), equalTo(CONSUMER));
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

        descriptor = new DeclarationDescriptor("i have spaces", "1.0").withConfig("default").getRootDeclaration();
        factory.createFrom(descriptor);
    }

    @Test(expected = IllegalArgumentException.class)
    public void configlessDescriptor()
    {

        factory.createFrom(new DeclarationDescriptor("noConfigs", "1.0"));
    }

    @Test
    public void postProcessorsInvoked() throws Exception
    {
        DescriberPostProcessor postProcessor1 = mock(DescriberPostProcessor.class);
        DescriberPostProcessor postProcessor2 = mock(DescriberPostProcessor.class);

        when(serviceRegistry.lookupProviders(same(DescriberPostProcessor.class), any(ClassLoader.class)))
                .thenReturn(Arrays.asList(postProcessor1, postProcessor2));

        factory = new DefaultExtensionFactory(serviceRegistry);
        factory.createFrom(descriptor);

        assertDescribingContext(postProcessor1);
        assertDescribingContext(postProcessor2);
    }

    private void assertDescribingContext(DescriberPostProcessor postProcessor)
    {
        ArgumentCaptor<DescribingContext> captor = ArgumentCaptor.forClass(DescribingContext.class);
        verify(postProcessor).postProcess(captor.capture());

        DescribingContext ctx = captor.getValue();
        assertThat(ctx, is(notNullValue()));
        assertThat(ctx.getDeclarationDescriptor(), is(sameInstance(descriptor)));
    }

    private void assertConsumeOperation(List<Operation> operations) throws NoSuchOperationException
    {
        Operation operation = operations.get(2);
        assertThat(operation, is(sameInstance(extension.getOperation(CONSUMER))));

        assertThat(operation.getName(), equalTo(CONSUMER));
        assertThat(operation.getDescription(), equalTo(GO_GET_THEM_TIGER));

        List<Parameter> parameters = operation.getParameters();
        assertThat(parameters, hasSize(2));
        assertParameter(parameters.get(0), OPERATION, THE_OPERATION_TO_USE, true, true, of(String.class), STRING, null);
        assertParameter(parameters.get(1), MTOM_ENABLED, MTOM_DESCRIPTION, true, false, of(Boolean.class), BOOLEAN, true);
    }

    private void assertBroadcastOperation(List<Operation> operations) throws NoSuchOperationException
    {
        Operation operation = operations.get(1);
        assertThat(operation, is(sameInstance(extension.getOperation(BROADCAST))));

        assertThat(operation.getName(), equalTo(BROADCAST));
        assertThat(operation.getDescription(), equalTo(BROADCAST_DESCRIPTION));

        List<Parameter> parameters = operation.getParameters();
        assertThat(parameters, hasSize(3));
        assertParameter(parameters.get(0), OPERATION, THE_OPERATION_TO_USE, true, true, of(List.class, String.class), LIST, null);
        assertParameter(parameters.get(1), MTOM_ENABLED, MTOM_DESCRIPTION, true, false, of(Boolean.class), BOOLEAN, true);
        assertParameter(parameters.get(2), CALLBACK, CALLBACK_DESCRIPTION, false, true, of(Operation.class), DataQualifier.OPERATION, null);
    }
    private void assertArglessOperation(List<Operation> operations) throws NoSuchOperationException
    {
        Operation operation = operations.get(0);
        assertThat(operation, is(sameInstance(extension.getOperation(ARG_LESS))));

        assertThat(operation.getName(), equalTo(ARG_LESS));
        assertThat(operation.getDescription(), equalTo(HAS_NO_ARGS));

        List<Parameter> parameters = operation.getParameters();
        assertThat(parameters.isEmpty(), is(true));
    }


    private void assertParameter(Parameter parameter,
                                 String name,
                                 String description,
                                 boolean acceptsExpressions,
                                 boolean required,
                                 DataType type,
                                 DataQualifier qualifier,
                                 Object defaultValue)
    {
        assertThat(parameter, is(notNullValue()));
        assertThat(parameter.getName(), equalTo(name));
        assertThat(parameter.getDescription(), equalTo(description));
        assertThat(parameter.isDynamic(), is(acceptsExpressions));
        assertThat(parameter.isRequired(), is(required));
        assertThat(parameter.getType(), equalTo(type));
        assertThat(parameter.getType().getQualifier(), is(qualifier));

        if (defaultValue != null)
        {
            assertThat(parameter.getDefaultValue(), equalTo(defaultValue));
        }
        else
        {
            assertThat(parameter.getDefaultValue(), is(nullValue()));
        }
    }

    private DeclarationDescriptor createDeclarationDescriptor()
    {
        return new TestWebServiceConsumerDeclarationReference().getDescriptor();
    }
}
