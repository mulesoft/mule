/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.registry;

import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.test.allure.AllureConstants.RegistryFeature.REGISTRY;
import static org.mule.test.allure.AllureConstants.RegistryFeature.TransfromersStory.TRANSFORMERS;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.internal.transformer.DefaultTransformersRegistry;
import org.mule.runtime.core.internal.transformer.builder.MockConverterBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Orange;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@SmallTest
@Feature(REGISTRY)
@Story(TRANSFORMERS)
public class DefaultTransformersRegistryTransformerLookupTestCase extends AbstractMuleTestCase {

  private static final DataType ORANGE_DATA_TYPE = DataType.fromType(Orange.class);

  private final Converter stringToOrange = new MockConverterBuilder().from(STRING).to(ORANGE_DATA_TYPE).build();
  private final Converter orangeToString = new MockConverterBuilder().from(ORANGE_DATA_TYPE).to(STRING).build();
  private final DefaultTransformersRegistry transformersRegistry = spy(new DefaultTransformersRegistry());

  @Before
  public void setUp() throws Exception {
    TransformerResolver transformerResolver = mock(TransformerResolver.class);
    when(transformerResolver.resolve(STRING, ORANGE_DATA_TYPE)).thenReturn(stringToOrange);
    when(transformerResolver.resolve(ORANGE_DATA_TYPE, STRING)).thenReturn(orangeToString);

    transformersRegistry.setTransformerResolvers(singletonList(transformerResolver));
    transformersRegistry.setTransformers(asList(orangeToString, stringToOrange));
    transformersRegistry.initialise();
  }

  @Test
  public void cachesTransformerResolvers() throws Exception {
    Transformer transformer1 = transformersRegistry.lookupTransformer(STRING, ORANGE_DATA_TYPE);
    Transformer transformer2 = transformersRegistry.lookupTransformer(ORANGE_DATA_TYPE, STRING);

    assertThat(transformer1, sameInstance(stringToOrange));
    assertThat(transformer2, sameInstance(orangeToString));
  }

  @Test
  public void cachesTransformers() throws Exception {
    List<Transformer> transformers = transformersRegistry.lookupTransformers(STRING, ORANGE_DATA_TYPE);

    assertThat(transformers, hasSize(1));
    assertThat(transformers.get(0), sameInstance(stringToOrange));
  }
}
