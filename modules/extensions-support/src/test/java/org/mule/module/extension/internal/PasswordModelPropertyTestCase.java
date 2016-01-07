/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.api.registry.ServiceRegistry;
import org.mule.extension.api.introspection.ExtensionFactory;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.introspection.ParameterModel;
import org.mule.extension.api.introspection.declaration.fluent.Descriptor;
import org.mule.extension.api.introspection.declaration.spi.ModelEnricher;
import org.mule.extension.api.introspection.property.PasswordModelProperty;
import org.mule.module.extension.internal.introspection.AnnotationsBasedDescriber;
import org.mule.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.module.extension.internal.introspection.enricher.PasswordModelEnricher;
import org.mule.module.extension.internal.runtime.connector.secure.SecureConnector;
import org.mule.registry.SpiServiceRegistry;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class PasswordModelPropertyTestCase extends AbstractMuleTestCase
{

    private ExtensionFactory extensionFactory;
    private ExtensionModel extensionModel;

    @Before
    public void setUp()
    {
        ClassLoader classLoader = getClass().getClassLoader();
        ServiceRegistry serviceRegistry = mock(ServiceRegistry.class);
        when(serviceRegistry.lookupProviders(ModelEnricher.class, classLoader)).thenReturn(asList(new PasswordModelEnricher()));

        extensionFactory = new DefaultExtensionFactory(new SpiServiceRegistry(), getClass().getClassLoader());
        Descriptor descriptor = new AnnotationsBasedDescriber(SecureConnector.class).describe(new DefaultDescribingContext()).getRootDeclaration();
        extensionModel = extensionFactory.createFrom(descriptor);
    }

    @Test
    public void verifyPasswordPropertyIsPopulatedAtConfig() throws Exception
    {
        List<ParameterModel> parameterModels = extensionModel.getConfigurationModels().get(0).getParameterModels();
        assertThat(parameterModels, hasSize(2));
        assertThat(parameterModels.get(1).getModelProperty(PasswordModelProperty.KEY), is(notNullValue()));
        assertThat(parameterModels.get(0).getModelProperty(PasswordModelProperty.KEY), is(nullValue()));

    }

    @Test
    public void verifyPasswordPropertyIsPopulatedAtProvider() throws Exception
    {
        assertThat(extensionModel.getConnectionProviders().get(0).getParameterModels().get(0).getModelProperty(PasswordModelProperty.KEY), is(notNullValue()));
    }


    @Test
    public void verifyPasswordPropertyIsPopulatedAtMethodParameter() throws Exception
    {
        assertThat(extensionModel.getOperationModels().get(0).getParameterModels().get(0).getModelProperty(PasswordModelProperty.KEY), is(notNullValue()));
    }

}
