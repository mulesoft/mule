/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.transformers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import org.mule.DefaultMuleContext;
import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.registry.TransformerResolver;
import org.mule.api.transformer.Converter;
import org.mule.config.builders.AbstractConfigurationBuilder;
import org.mule.registry.MuleRegistryHelper;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class ConvertersNotRegisteredTwiceTestCase extends FunctionalTestCase
{

    private MuleRegistryHelper registryHelper;

    @Override
    protected String[] getConfigFiles()
    {
        return new String[] {};
    }


    @Override
    protected void addBuilders(List<ConfigurationBuilder> builders)
    {
        super.addBuilders(builders);
        builders.add(0, new AbstractConfigurationBuilder()
        {
            @Override
            protected void doConfigure(MuleContext muleContext) throws Exception
            {
                registryHelper = (MuleRegistryHelper) muleContext.getRegistry();
                registryHelper = spy(registryHelper);
                ((DefaultMuleContext) muleContext).setMuleRegistry(registryHelper);
            }
        });
    }

    @Test
    public void noDuplicates()
    {
        ArgumentCaptor<TransformerResolver> transformerResolverCaptor = ArgumentCaptor.forClass(TransformerResolver.class);
        verify(registryHelper, atLeastOnce()).registerTransformerResolver(transformerResolverCaptor.capture());
        assertNoDuplicatesNorEmpty(transformerResolverCaptor.getAllValues());

        ArgumentCaptor<Converter> converterArgumentCaptor = ArgumentCaptor.forClass(Converter.class);
        verify(registryHelper, atLeastOnce()).notifyTransformerResolvers(converterArgumentCaptor.capture(), same(TransformerResolver.RegistryAction.ADDED));
        assertNoDuplicatesNorEmpty(converterArgumentCaptor.getAllValues());
    }

    private <T> void assertNoDuplicatesNorEmpty(Collection<T> collection)
    {
        assertThat(collection.isEmpty(), is(false));

        Set<T> noDuplicates = new HashSet<>(collection);
        assertThat(noDuplicates, hasSize(collection.size()));
    }
}
