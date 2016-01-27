/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.api.registry.ServiceRegistry;
import org.mule.extension.api.introspection.ExtensionFactory;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.introspection.declaration.fluent.Descriptor;
import org.mule.extension.api.introspection.declaration.spi.ModelEnricher;
import org.mule.module.extension.internal.introspection.describer.AnnotationsBasedDescriber;
import org.mule.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.registry.SpiServiceRegistry;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;

/**
 * Base class to test {@link org.mule.module.extension.internal.introspection.enricher.AbstractAnnotatedParameterModelEnricher}s
 */
public abstract class AbstractAnnotatedParameterModelEnricherTest extends AbstractMuleTestCase
{

    private ExtensionFactory extensionFactory;
    private ExtensionModel extensionModel;

    @Before
    public void setUp()
    {
        ServiceRegistry serviceRegistry = mock(ServiceRegistry.class);
        when(serviceRegistry.lookupProviders(same(ModelEnricher.class), any(ClassLoader.class))).thenReturn(asList(getModelEnricherUnderTest()));

        extensionFactory = new DefaultExtensionFactory(new SpiServiceRegistry(), getClass().getClassLoader());
        Descriptor descriptor = new AnnotationsBasedDescriber(getExtensionForTest()).describe(new DefaultDescribingContext()).getRootDeclaration();
        extensionModel = extensionFactory.createFrom(descriptor);
    }

    public ExtensionModel getExtensionModel()
    {
        return extensionModel;
    }

    protected abstract Class<?> getExtensionForTest();

    protected abstract ModelEnricher getModelEnricherUnderTest();

}