/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.registry;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.core.internal.registry.TransformerResolver.RegistryAction.ADDED;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConvertersNotRegisteredTwiceTestCase extends AbstractMuleContextTestCase {

  private MuleRegistryHelper registryHelper;

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    super.addBuilders(builders);
    builders.add(0, new AbstractConfigurationBuilder() {

      @Override
      protected void doConfigure(MuleContext muleContext) throws Exception {
        registryHelper = (MuleRegistryHelper) ((MuleContextWithRegistries) muleContext).getRegistry();
        registryHelper = spy(registryHelper);
        ((DefaultMuleContext) muleContext).setMuleRegistry(registryHelper);
      }
    });
  }

  @Test
  public void noDuplicates() {
    ArgumentCaptor<TransformerResolver> transformerResolverCaptor = forClass(TransformerResolver.class);
    verify(registryHelper, atLeastOnce()).registerTransformerResolver(transformerResolverCaptor.capture());
    assertNoDuplicatesNorEmpty(transformerResolverCaptor.getAllValues());

    ArgumentCaptor<Converter> converterArgumentCaptor = forClass(Converter.class);
    verify(registryHelper, atLeastOnce()).notifyTransformerResolvers(converterArgumentCaptor.capture(), same(ADDED));
    assertNoDuplicatesNorEmpty(converterArgumentCaptor.getAllValues());
  }

  private <T> void assertNoDuplicatesNorEmpty(Collection<T> collection) {
    assertThat(collection, is(not(empty())));

    Set<T> noDuplicates = new HashSet<>(collection);
    assertThat(noDuplicates, hasSize(collection.size()));
  }
}
