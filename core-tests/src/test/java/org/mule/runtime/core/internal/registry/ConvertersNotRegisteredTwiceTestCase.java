/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.registry;

import static org.mule.runtime.core.internal.registry.TransformerResolver.RegistryAction.ADDED;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.transformer.DefaultTransformersRegistry;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import org.mockito.ArgumentCaptor;

public class ConvertersNotRegisteredTwiceTestCase extends AbstractMuleContextTestCase {

  private final DefaultTransformersRegistry transformersRegistry = spy(new DefaultTransformersRegistry());

  @Test
  public void noDuplicates() throws InitialisationException {
    transformersRegistry
        .setTransformers(new ArrayList<>(((MuleContextWithRegistry) muleContext).getRegistry().lookupObjects(Transformer.class)));
    transformersRegistry.initialise();

    ArgumentCaptor<Converter> converterArgumentCaptor = forClass(Converter.class);
    verify(transformersRegistry, atLeastOnce()).notifyTransformerResolvers(converterArgumentCaptor.capture(), same(ADDED));
    assertNoDuplicatesNorEmpty(converterArgumentCaptor.getAllValues());
  }

  private <T> void assertNoDuplicatesNorEmpty(Collection<T> collection) {
    assertThat(collection, is(not(empty())));

    Set<T> noDuplicates = new HashSet<>(collection);
    assertThat(noDuplicates, hasSize(collection.size()));
  }
}
