/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
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
import static org.mule.extension.api.introspection.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.extension.api.introspection.ExpressionSupport.REQUIRED;
import static org.mule.extension.api.introspection.ExpressionSupport.SUPPORTED;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.ADDRESS;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.ARG_LESS;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.BROADCAST;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.BROADCAST_DESCRIPTION;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.CALLBACK;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.CALLBACK_DESCRIPTION;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.CONFIG_DESCRIPTION;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.CONFIG_NAME;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.CONNECTION_PROVIDER_CONFIG_TYPE;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.CONNECTION_PROVIDER_CONNECTOR_TYPE;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.CONNECTION_PROVIDER_DESCRIPTION;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.CONNECTION_PROVIDER_NAME;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.CONSUMER;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.DEFAULT_PORT;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.GO_GET_THEM_TIGER;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.HAS_NO_ARGS;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.LISTENER;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.LISTEN_DESCRIPTION;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.MTOM_DESCRIPTION;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.MTOM_ENABLED;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.MULESOFT;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.OPERATION;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.PASSWORD;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.PASSWORD_DESCRIPTION;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.PORT;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.PORT_DESCRIPTION;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.SERVICE;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.SERVICE_ADDRESS;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.SERVICE_NAME;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.SERVICE_PORT;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.THE_OPERATION_TO_USE;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.URI_TO_FIND_THE_WSDL;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.URL;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.URL_DESCRIPTION;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.USERNAME;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.USERNAME_DESCRIPTION;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.VERSION;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.WSDL_LOCATION;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.WS_CONSUMER;
import static org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference.WS_CONSUMER_DESCRIPTION;
import static org.mule.metadata.java.utils.JavaTypeUtils.getType;
import static org.mule.module.extension.internal.ExtensionProperties.TARGET_ATTRIBUTE;
import static org.mule.module.extension.internal.util.ExtensionsTestUtils.TYPE_BUILDER;
import static org.mule.module.extension.internal.util.ExtensionsTestUtils.arrayOf;
import static org.mule.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import org.mule.api.registry.ServiceRegistry;
import org.mule.extension.api.exception.IllegalModelDefinitionException;
import org.mule.extension.api.exception.NoSuchConfigurationException;
import org.mule.extension.api.exception.NoSuchOperationException;
import org.mule.extension.api.introspection.ConfigurationFactory;
import org.mule.extension.api.introspection.ConfigurationModel;
import org.mule.extension.api.introspection.ConnectionProviderModel;
import org.mule.extension.api.introspection.ExpressionSupport;
import org.mule.extension.api.introspection.ExtensionFactory;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.introspection.Interceptable;
import org.mule.extension.api.introspection.OperationModel;
import org.mule.extension.api.introspection.ParameterModel;
import org.mule.extension.api.introspection.SourceModel;
import org.mule.extension.api.introspection.declaration.DescribingContext;
import org.mule.extension.api.introspection.declaration.fluent.DeclarationDescriptor;
import org.mule.extension.api.introspection.declaration.spi.ModelEnricher;
import org.mule.extension.api.introspection.declaration.tck.TestWebServiceConsumerDeclarationReference;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.BinaryType;
import org.mule.metadata.api.model.BooleanType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NullType;
import org.mule.metadata.api.model.NumberType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.module.extension.internal.exception.IllegalOperationModelDefinitionException;
import org.mule.module.extension.internal.exception.IllegalParameterModelDefinitionException;
import org.mule.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.InputStream;
import java.io.Serializable;
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

    private final TestWebServiceConsumerDeclarationReference reference = new TestWebServiceConsumerDeclarationReference();
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
        assertThat(extensionModel.getConfigurationModels(), hasSize(1));
        assertThat(extensionModel.getVendor(), equalTo(MULESOFT));

        verify(serviceRegistry).lookupProviders(any(Class.class), any(ClassLoader.class));
    }

    @Test
    public void defaultConfiguration() throws Exception
    {
        ConfigurationModel configurationModel = extensionModel.getConfigurationModel(CONFIG_NAME);
        assertThat(configurationModel, is(notNullValue()));
        assertThat(configurationModel.getName(), equalTo(CONFIG_NAME));
        assertThat(configurationModel.getDescription(), equalTo(CONFIG_DESCRIPTION));
        assertThat(configurationModel.getExtensionModel(), is(sameInstance(extensionModel)));

        List<ParameterModel> parameterModels = configurationModel.getParameterModels();
        assertThat(parameterModels, hasSize(4));
        assertParameter(parameterModels.get(0), ADDRESS, SERVICE_ADDRESS, SUPPORTED, true, toMetadataType(String.class), StringType.class, null);
        assertParameter(parameterModels.get(1), PORT, SERVICE_PORT, SUPPORTED, true, toMetadataType(String.class), StringType.class, null);
        assertParameter(parameterModels.get(2), SERVICE, SERVICE_NAME, SUPPORTED, true, toMetadataType(String.class), StringType.class, null);
        assertParameter(parameterModels.get(3), WSDL_LOCATION, URI_TO_FIND_THE_WSDL, NOT_SUPPORTED, true, toMetadataType(String.class), StringType.class, null);
    }

    @Test
    public void onlyOneConfig() throws Exception
    {
        assertThat(extensionModel.getConfigurationModels(), hasSize(1));
        assertThat(extensionModel.getConfigurationModels().get(0), is(sameInstance(extensionModel.getConfigurationModel(CONFIG_NAME))));
    }

    @Test(expected = NoSuchConfigurationException.class)
    public void noSuchConfiguration() throws Exception
    {
        extensionModel.getConfigurationModel("fake");
    }

    @Test(expected = NoSuchOperationException.class)
    public void noSuchOperation() throws Exception
    {
        extensionModel.getOperationModel("fake");
    }

    @Test
    public void operations() throws Exception
    {
        List<OperationModel> operationModels = extensionModel.getOperationModels();
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
        ConfigurationFactory mockInstantiator = mock(ConfigurationFactory.class);

        final String defaultConfiguration = "default";
        final String beta = "beta";
        final String alpha = "alpha";

        ExtensionModel extensionModel = factory.createFrom(new DeclarationDescriptor().named("test").onVersion("1.0")
                                                                   .fromVendor("MuleSoft")
                                                                   .withConfig(defaultConfiguration).describedAs(defaultConfiguration).createdWith(mockInstantiator)
                                                                   .withConfig(beta).describedAs(beta).createdWith(mockInstantiator)
                                                                   .withConfig(alpha).describedAs(alpha).createdWith(mockInstantiator));

        List<ConfigurationModel> configurationModels = extensionModel.getConfigurationModels();
        assertThat(configurationModels, hasSize(3));
        assertThat(configurationModels.get(1).getName(), equalTo(alpha));
        assertThat(configurationModels.get(2).getName(), equalTo(beta));
    }

    @Test
    public void operationsAlphaSorted()
    {
        assertThat(extensionModel.getOperationModels(), hasSize(3));
        assertThat(extensionModel.getOperationModels().get(0).getName(), equalTo(ARG_LESS));
        assertThat(extensionModel.getOperationModels().get(1).getName(), equalTo(BROADCAST));
        assertThat(extensionModel.getOperationModels().get(2).getName(), equalTo(CONSUMER));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nameClashes()
    {
        factory.createFrom(descriptor.withConfig(CONFIG_NAME).createdWith(mock(ConfigurationFactory.class)).describedAs(""));
    }

    @Test(expected = IllegalParameterModelDefinitionException.class)
    public void operationWithParameterNamedName()
    {
        factory.createFrom(descriptor.withOperation("invalidOperation").describedAs("")
                                   .with().requiredParameter("name").ofType(String.class));
    }

    @Test(expected = IllegalParameterModelDefinitionException.class)
    public void fixedParameterWithExpressionDefault()
    {
        factory.createFrom(descriptor.withOperation("invalidOperation").describedAs("")
                                   .with().optionalParameter("fixed").ofType(String.class)
                                   .withExpressionSupport(NOT_SUPPORTED)
                                   .defaultingTo("#['hello']"));
    }

    @Test(expected = IllegalOperationModelDefinitionException.class)
    public void operationWithParameterNamedTarget()
    {
        factory.createFrom(descriptor.withOperation("invalidOperation").describedAs("")
                                   .with().optionalParameter(TARGET_ATTRIBUTE).ofType(String.class));
    }

    @Test(expected = IllegalParameterModelDefinitionException.class)
    public void expressionParameterWithFixedValue()
    {
        factory.createFrom(descriptor.withOperation("invalidOperation").describedAs("")
                                   .with().optionalParameter("expression").ofType(String.class)
                                   .withExpressionSupport(REQUIRED)
                                   .defaultingTo("static"));
    }

    @Test(expected = IllegalOperationModelDefinitionException.class)
    public void operationWithNoReturnType()
    {
        factory.createFrom(descriptor.withOperation("noReturn").describedAs(""));
    }

    @Test(expected = IllegalModelDefinitionException.class)
    public void nullVendor()
    {
        descriptor = new DeclarationDescriptor()
                .named("IWillExplode")
                .onVersion("1.2.3")
                .withConfig("default")
                .createdWith(mock(ConfigurationFactory.class))
                .getRootDeclaration();

        factory.createFrom(descriptor);
    }

    @Test
    public void configlessDescriptor()
    {
        factory.createFrom(new DeclarationDescriptor().named("noConfigs").onVersion("1.0").fromVendor("MuleSoft"));
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

    @Test
    public void executorsAreInterceptable()
    {
        for (OperationModel operation : extensionModel.getOperationModels())
        {
            assertThat(operation.getExecutor().createExecutor(), is(instanceOf(Interceptable.class)));
        }
    }

    @Test
    public void connectionProviders()
    {
        assertThat(extensionModel.getConnectionProviders(), hasSize(1));
        ConnectionProviderModel connectionProvider = extensionModel.getConnectionProviders().get(0);
        assertThat(connectionProvider, is(notNullValue()));
        assertThat(connectionProvider.getName(), is(CONNECTION_PROVIDER_NAME));
        assertThat(connectionProvider.getDescription(), is(CONNECTION_PROVIDER_DESCRIPTION));
        assertThat(connectionProvider.getConnectionProviderFactory(), is(sameInstance(reference.getConnectionProviderFactory())));
        assertThat(connectionProvider.getConfigurationType(), is(sameInstance(CONNECTION_PROVIDER_CONFIG_TYPE)));
        assertThat(connectionProvider.getConnectionType(), is(sameInstance(CONNECTION_PROVIDER_CONNECTOR_TYPE)));

        List<ParameterModel> parameters = connectionProvider.getParameterModels();
        assertParameter(parameters.get(0), USERNAME, USERNAME_DESCRIPTION, SUPPORTED, true, toMetadataType(String.class), StringType.class, null);
        assertParameter(parameters.get(1), PASSWORD, PASSWORD_DESCRIPTION, SUPPORTED, true, toMetadataType(String.class), StringType.class, null);
    }

    @Test
    public void messageSources()
    {
        assertThat(extensionModel.getSourceModels(), hasSize(1));
        SourceModel sourceModel = extensionModel.getSourceModels().get(0);
        assertThat(sourceModel, is(notNullValue()));
        assertThat(sourceModel.getName(), is(LISTENER));
        assertThat(sourceModel.getDescription(), is(LISTEN_DESCRIPTION));
        assertThat(sourceModel.getSourceFactory().createSource(), is(sameInstance(reference.getSource())));
        assertThat(getType(sourceModel.getReturnType()), is(equalTo(InputStream.class)));
        assertThat(getType(sourceModel.getAttributesType()), is(equalTo(Serializable.class)));

        List<ParameterModel> parameters = sourceModel.getParameterModels();
        assertParameter(parameters.get(0), URL, URL_DESCRIPTION, SUPPORTED, true, toMetadataType(String.class), StringType.class, null);
        assertParameter(parameters.get(1), PORT, PORT_DESCRIPTION, SUPPORTED, false, toMetadataType(Integer.class), NumberType.class, DEFAULT_PORT);
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
        assertThat(operationModel, is(sameInstance(extensionModel.getOperationModel(CONSUMER))));
        assertDataType(operationModel.getReturnType(), InputStream.class, BinaryType.class);

        assertThat(operationModel.getName(), equalTo(CONSUMER));
        assertThat(operationModel.getDescription(), equalTo(GO_GET_THEM_TIGER));

        List<ParameterModel> parameterModels = operationModel.getParameterModels();
        assertThat(parameterModels, hasSize(2));
        assertParameter(parameterModels.get(0), OPERATION, THE_OPERATION_TO_USE, SUPPORTED, true, toMetadataType(String.class), StringType.class, null);
        assertParameter(parameterModels.get(1), MTOM_ENABLED, MTOM_DESCRIPTION, SUPPORTED, false, toMetadataType(Boolean.class), BooleanType.class, true);
    }

    private void assertBroadcastOperation(List<OperationModel> operationModels) throws NoSuchOperationException
    {
        OperationModel operationModel = operationModels.get(1);
        assertThat(operationModel, is(sameInstance(extensionModel.getOperationModel(BROADCAST))));
        assertDataType(operationModel.getReturnType(), void.class, NullType.class);

        assertThat(operationModel.getName(), equalTo(BROADCAST));
        assertThat(operationModel.getDescription(), equalTo(BROADCAST_DESCRIPTION));

        List<ParameterModel> parameterModels = operationModel.getParameterModels();
        assertThat(parameterModels, hasSize(3));
        assertParameter(parameterModels.get(0), OPERATION, THE_OPERATION_TO_USE, SUPPORTED, true, arrayOf(List.class, TYPE_BUILDER.stringType().id(String.class.getName())), ArrayType.class, null);
        assertParameter(parameterModels.get(1), MTOM_ENABLED, MTOM_DESCRIPTION, SUPPORTED, false, toMetadataType(Boolean.class), BooleanType.class, true);
        assertParameter(parameterModels.get(2), CALLBACK, CALLBACK_DESCRIPTION, REQUIRED, true, toMetadataType(OperationModel.class), ObjectType.class, null);
    }

    private void assertArglessOperation(List<OperationModel> operationModels) throws NoSuchOperationException
    {
        OperationModel operationModel = operationModels.get(0);
        assertThat(operationModel, is(sameInstance(extensionModel.getOperationModel(ARG_LESS))));
        assertDataType(operationModel.getReturnType(), int.class, NumberType.class);

        assertThat(operationModel.getName(), equalTo(ARG_LESS));
        assertThat(operationModel.getDescription(), equalTo(HAS_NO_ARGS));

        List<ParameterModel> parameterModels = operationModel.getParameterModels();
        assertThat(parameterModels.isEmpty(), is(true));
    }


    private void assertParameter(ParameterModel parameterModel,
                                 String name,
                                 String description,
                                 ExpressionSupport expressionSupport,
                                 boolean required,
                                 MetadataType metadataType,
                                 Class<? extends MetadataType> qualifier,
                                 Object defaultValue)
    {
        assertThat(parameterModel, is(notNullValue()));
        assertThat(parameterModel.getName(), equalTo(name));
        assertThat(parameterModel.getDescription(), equalTo(description));
        assertThat(parameterModel.getExpressionSupport(), is(expressionSupport));
        assertThat(parameterModel.isRequired(), is(required));
        assertThat(parameterModel.getType(), equalTo(metadataType));
        assertThat(parameterModel.getType(), is(instanceOf(qualifier)));

        if (defaultValue != null)
        {
            assertThat(parameterModel.getDefaultValue(), equalTo(defaultValue));
        }
        else
        {
            assertThat(parameterModel.getDefaultValue(), is(nullValue()));
        }
    }

    private void assertDataType(MetadataType metadataType, Class<?> expectedRawType, Class<? extends MetadataType> typeQualifier)
    {
        assertThat(metadataType, is(instanceOf(typeQualifier)));
        assertThat(expectedRawType.isAssignableFrom(getType(metadataType)), is(true));
    }

    private DeclarationDescriptor createDeclarationDescriptor()
    {
        return reference.getDescriptor();
    }
}
