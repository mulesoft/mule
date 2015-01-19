/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.registry.ServiceRegistry;
import org.mule.extensions.ExtensionsManager;
import org.mule.extensions.introspection.Describer;
import org.mule.extensions.introspection.Extension;
import org.mule.extensions.introspection.ExtensionFactory;
import org.mule.extensions.introspection.declaration.Construct;
import org.mule.module.extensions.internal.introspection.ExtensionDiscoverer;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ExtensionDiscovererTestCase extends AbstractMuleTestCase
{

    @Mock
    private ExtensionsManager extensionsManager;

    @Mock
    private ExtensionFactory extensionFactory;

    @Mock
    private ServiceRegistry serviceRegistry;

    @Mock
    private Construct construct;

    @Mock
    private Describer describer;

    @Mock
    private Extension extension;

    private ExtensionDiscoverer discoverer;

    @Before
    public void setUp()
    {
        discoverer = new DefaultExtensionDiscoverer(extensionFactory, serviceRegistry);
    }

    @Test
    public void scan() throws Exception
    {
        when(serviceRegistry.lookupProviders(Describer.class, getClass().getClassLoader())).thenReturn(Arrays.asList(describer));
        when(describer.describe()).thenReturn(construct);
        when(extensionFactory.createFrom(construct)).thenReturn(extension);

        List<Extension> extensions = discoverer.discover(getClass().getClassLoader());
        assertThat(extensions, hasSize(1));

        assertThat(extensions.get(0), is(sameInstance(extension)));

        verify(serviceRegistry).lookupProviders(Describer.class, getClass().getClassLoader());
        verify(describer).describe();
        verify(extensionFactory).createFrom(construct);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullClassLoader()
    {
        discoverer.discover(null);
    }
}
