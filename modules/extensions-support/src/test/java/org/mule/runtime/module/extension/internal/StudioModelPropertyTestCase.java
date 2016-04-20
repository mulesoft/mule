/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.config.MuleManifest.getProductVersion;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.extension.api.introspection.ExtensionFactory;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.spi.ModelEnricher;
import org.mule.runtime.extension.api.introspection.property.StudioModelProperty;
import org.mule.runtime.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.runtime.module.extension.internal.introspection.describer.AnnotationsBasedDescriber;
import org.mule.runtime.module.extension.internal.introspection.enricher.StudioModelEnricher;
import org.mule.runtime.module.extension.internal.introspection.version.StaticVersionResolver;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

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

        extensionFactory = new DefaultExtensionFactory(serviceRegistry, getClass().getClassLoader());
    }

    @Test
    public void verifyPropertyIsPopulated() throws Exception
    {
        ExtensionDeclarer declarer = new AnnotationsBasedDescriber(HeisenbergExtension.class, new StaticVersionResolver(getProductVersion())).describe(new DefaultDescribingContext());
        ExtensionModel extensionModel = extensionFactory.createFrom(declarer);
        StudioModelProperty studioModelProperty = extensionModel.getModelProperty(StudioModelProperty.class).get();
        assertThat(studioModelProperty.getEditorFileName(), is(""));
        assertThat(studioModelProperty.isDerived(), is(true));
    }
}
