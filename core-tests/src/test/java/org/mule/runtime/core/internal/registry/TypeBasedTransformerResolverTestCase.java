/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.transformer.ResolverException;
import org.mule.runtime.core.internal.transformer.builder.MockConverterBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

@SmallTest
public class TypeBasedTransformerResolverTestCase extends AbstractMuleTestCase {

  private MuleContextWithRegistry muleContext = mock(MuleContextWithRegistry.class, RETURNS_DEEP_STUBS);
  private MuleConfiguration muleConfiguration = mock(MuleConfiguration.class);

  public static class A {
  }

  public static class B {
  }

  private DataType dataTypeA = DataType.fromType(A.class);
  private DataType dataTypeB = DataType.fromType(B.class);

  @Before
  public void setUp() throws Exception {
    when(muleContext.getConfiguration()).thenReturn(muleConfiguration);
  }

  @Test
  public void doesNotFailIfCannotResolveType() throws ResolverException, TransformerException {
    MuleRegistry muleRegistry = mock(MuleRegistry.class);
    when(muleContext.getRegistry()).thenReturn(muleRegistry);
    ArrayList<Transformer> transformers = new ArrayList<>();
    when(muleRegistry.lookupTransformers(dataTypeA, dataTypeB)).thenReturn(transformers);
    TypeBasedTransformerResolver resolver = new TypeBasedTransformerResolver();
    resolver.setMuleContext(muleContext);

    Transformer resolvedTransformer = resolver.resolve(dataTypeA, dataTypeB);
    assertNull(resolvedTransformer);
  }

  @Test
  public void resolvesTypeWithOneMatchingTransformer() throws ResolverException, TransformerException {
    MuleRegistry muleRegistry = mock(MuleRegistry.class);
    when(muleContext.getRegistry()).thenReturn(muleRegistry);
    Transformer aToBConverter = new MockConverterBuilder().from(dataTypeA).to(dataTypeB).build();

    ArrayList<Transformer> transformers = new ArrayList<>();
    transformers.add(aToBConverter);
    when(muleRegistry.lookupTransformers(dataTypeA, dataTypeB)).thenReturn(transformers);


    TypeBasedTransformerResolver resolver = new TypeBasedTransformerResolver();
    resolver.setMuleContext(muleContext);

    Transformer resolvedTransformer = resolver.resolve(dataTypeA, dataTypeB);
    assertEquals(aToBConverter, resolvedTransformer);
  }

  @Test
  public void resolvesTypeWithTwoMatchingTransformer() throws ResolverException, TransformerException {
    MuleRegistry muleRegistry = mock(MuleRegistry.class);
    when(muleContext.getRegistry()).thenReturn(muleRegistry);
    Transformer aToBConverter = new MockConverterBuilder().from(dataTypeA).to(dataTypeB).weighting(1).build();
    Transformer betterAToBConverter = new MockConverterBuilder().from(dataTypeA).to(dataTypeB).weighting(2).build();

    ArrayList<Transformer> transformers = new ArrayList<>();
    transformers.add(aToBConverter);
    transformers.add(betterAToBConverter);
    when(muleRegistry.lookupTransformers(dataTypeA, dataTypeB)).thenReturn(transformers);


    TypeBasedTransformerResolver resolver = new TypeBasedTransformerResolver();
    resolver.setMuleContext(muleContext);

    Transformer resolvedTransformer = resolver.resolve(dataTypeA, dataTypeB);
    assertEquals(betterAToBConverter, resolvedTransformer);
  }

  @Test
  public void fallbacksNotRegistered() throws Exception {
    TypeBasedTransformerResolver resolver = new TypeBasedTransformerResolver();
    resolver.setMuleContext(muleContext);
    resolver.initialise();

    verify(muleContext, never()).getRegistry();
  }
}
