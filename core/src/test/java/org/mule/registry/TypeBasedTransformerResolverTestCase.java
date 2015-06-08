/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.registry.ResolverException;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transformer.builder.MockConverterBuilder;
import org.mule.transformer.types.SimpleDataType;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class TypeBasedTransformerResolverTestCase extends AbstractMuleTestCase
{

    private MuleContext muleContext = mock(MuleContext.class);
    private MuleConfiguration muleConfiguration = mock(MuleConfiguration.class);

    public static class A
    {
    }

    public static class B
    {
    }

    private DataType<Object> dataTypeA = new SimpleDataType<Object>(A.class);
    private DataType<Object> dataTypeB = new SimpleDataType<Object>(B.class);

    @Before
    public void setUp() throws Exception
    {
        when(muleConfiguration.useExtendedTransformations()).thenReturn(false);
        when(muleContext.getConfiguration()).thenReturn(muleConfiguration);
    }

    @Test
    public void doesNotFailIfCannotResolveType() throws ResolverException, TransformerException
    {
        MuleRegistry muleRegistry = mock(MuleRegistry.class);
        when(muleContext.getRegistry()).thenReturn(muleRegistry);
        ArrayList<Transformer> transformers = new ArrayList<Transformer>();
        when(muleRegistry.lookupTransformers(dataTypeA, dataTypeB)).thenReturn(transformers);
        TypeBasedTransformerResolver resolver = new TypeBasedTransformerResolver();
        resolver.setMuleContext(muleContext);

        Transformer resolvedTransformer = resolver.resolve(dataTypeA, dataTypeB);
        assertNull(resolvedTransformer);
    }

    @Test
    public void resolvesTypeWithOneMatchingTransformer() throws ResolverException, TransformerException
    {
        MuleRegistry muleRegistry = mock(MuleRegistry.class);
        when(muleContext.getRegistry()).thenReturn(muleRegistry);
        Transformer aToBConverter = new MockConverterBuilder().from(dataTypeA).to(dataTypeB).build();

        ArrayList<Transformer> transformers = new ArrayList<Transformer>();
        transformers.add(aToBConverter);
        when(muleRegistry.lookupTransformers(dataTypeA, dataTypeB)).thenReturn(transformers);


        TypeBasedTransformerResolver resolver = new TypeBasedTransformerResolver();
        resolver.setMuleContext(muleContext);

        Transformer resolvedTransformer = resolver.resolve(dataTypeA, dataTypeB);
        assertEquals(aToBConverter, resolvedTransformer);
    }

    @Test
    public void resolvesTypeWithTwoMatchingTransformer() throws ResolverException, TransformerException
    {
        MuleRegistry muleRegistry = mock(MuleRegistry.class);
        when(muleContext.getRegistry()).thenReturn(muleRegistry);
        Transformer aToBConverter = new MockConverterBuilder().from(dataTypeA).to(dataTypeB).weighting(1).build();
        Transformer betterAToBConverter = new MockConverterBuilder().from(dataTypeA).to(dataTypeB).weighting(2).build();

        ArrayList<Transformer> transformers = new ArrayList<Transformer>();
        transformers.add(aToBConverter);
        transformers.add(betterAToBConverter);
        when(muleRegistry.lookupTransformers(dataTypeA, dataTypeB)).thenReturn(transformers);


        TypeBasedTransformerResolver resolver = new TypeBasedTransformerResolver();
        resolver.setMuleContext(muleContext);

        Transformer resolvedTransformer = resolver.resolve(dataTypeA, dataTypeB);
        assertEquals(betterAToBConverter, resolvedTransformer);
    }

    @Test
    public void fallbacksNotRegistered() throws Exception
    {
        TypeBasedTransformerResolver resolver = new TypeBasedTransformerResolver();
        resolver.setMuleContext(muleContext);
        resolver.initialise();

        verify(muleContext, never()).getRegistry();
    }

}
