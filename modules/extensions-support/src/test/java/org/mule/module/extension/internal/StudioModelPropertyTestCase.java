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
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.api.registry.ServiceRegistry;
import org.mule.extension.api.introspection.ExtensionFactory;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.introspection.declaration.fluent.Descriptor;
import org.mule.extension.api.introspection.declaration.spi.ModelEnricher;
import org.mule.extension.api.introspection.property.StudioModelProperty;
import org.mule.module.extension.HeisenbergExtension;
import org.mule.module.extension.internal.introspection.describer.AnnotationsBasedDescriber;
import org.mule.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.module.extension.internal.introspection.enricher.StudioModelEnricher;
import org.mule.registry.SpiServiceRegistry;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class StudioModelPropertyTestCase extends AbstractMuleTestCase
{

    private ExtensionFactory extensionFactory;

    @Before
    public void setUp()
    {
        ClassLoader classLoader = getClass().getClassLoader();
        ServiceRegistry serviceRegistry = mock(ServiceRegistry.class);
        when(serviceRegistry.lookupProviders(ModelEnricher.class, classLoader)).thenReturn(asList(new StudioModelEnricher()));

        extensionFactory = new DefaultExtensionFactory(new SpiServiceRegistry(), getClass().getClassLoader());
    }

    @Test
    public void verifyPropertyIsPopulated() throws Exception
    {
        Descriptor descriptor = new AnnotationsBasedDescriber(HeisenbergExtension.class).describe(new DefaultDescribingContext()).getRootDeclaration();
        ExtensionModel extensionModel = extensionFactory.createFrom(descriptor);
        StudioModelProperty studioModelProperty = extensionModel.getModelProperty(StudioModelProperty.KEY);
        assertThat(studioModelProperty, is(notNullValue()));
        assertThat(studioModelProperty.getEditorFileName(), is(""));
        assertThat(studioModelProperty.isDerived(), is(true));
    }
}
