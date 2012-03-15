/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.api.MuleContext;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.registry.ResolverException;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transformer.types.SimpleDataType;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class TypeBasedTransformerResolverTestCase extends AbstractMuleTestCase
{

    private MuleContext muleContext = mock(MuleContext.class);
    private MuleConfiguration muleConfiguration = mock(MuleConfiguration.class);

    public static class A
    {

        private final String value;

        public A(String value)
        {
            this.value = value;
        }
    }

    public static class B
    {

        private final String value;

        public B(String value)
        {
            this.value = value;
        }
    }

    public static class C
    {

        private final String value;

        public C(String value)
        {
            this.value = value;
        }
    }

    private DataType<Object> dataTypeA = new SimpleDataType<Object>(A.class);
    private DataType<Object> dataTypeB = new SimpleDataType<Object>(B.class);
    private DataType<Object> dataTypeC = new SimpleDataType<Object>(C.class);

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
        Transformer aToBConverter = createMockConverter(dataTypeA, dataTypeB);

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
        Transformer aToBConverter = createMockConverter(1, dataTypeA, dataTypeB);
        Transformer betterAToBConverter = createMockConverter(2, dataTypeA, dataTypeB);

        ArrayList<Transformer> transformers = new ArrayList<Transformer>();
        transformers.add(aToBConverter);
        transformers.add(betterAToBConverter);
        when(muleRegistry.lookupTransformers(dataTypeA, dataTypeB)).thenReturn(transformers);


        TypeBasedTransformerResolver resolver = new TypeBasedTransformerResolver();
        resolver.setMuleContext(muleContext);

        Transformer resolvedTransformer = resolver.resolve(dataTypeA, dataTypeB);
        assertEquals(betterAToBConverter, resolvedTransformer);
    }

    private Transformer createMockConverter(DataType returnType, DataType... sourceTypes)
    {
        Transformer converter = mock(MockConverter.class);
        doReturn(returnType).when(converter).getReturnDataType();
        doReturn(Arrays.asList(sourceTypes)).when(converter).getSourceDataTypes();

        return converter;
    }

    private Transformer createMockConverter(int weight, DataType returnType, DataType... sourceTypes)
    {
        MockConverter converter = mock(MockConverter.class);
        doReturn(returnType).when(converter).getReturnDataType();
        doReturn(Arrays.asList(sourceTypes)).when(converter).getSourceDataTypes();
        doReturn(weight).when(converter).getPriorityWeighting();

        return converter;
    }

    private interface MockConverter extends Transformer, DiscoverableTransformer
    {
    }
}
