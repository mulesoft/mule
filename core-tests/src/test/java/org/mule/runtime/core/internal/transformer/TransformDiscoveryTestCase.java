/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer;

import static org.mule.test.allure.AllureConstants.RegistryFeature.REGISTRY;
import static org.mule.test.allure.AllureConstants.RegistryFeature.TransfromersStory.TRANSFORMERS;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.internal.registry.TypeBasedTransformerResolver;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.tck.testmodels.fruit.RedApple;

import java.nio.charset.Charset;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(REGISTRY)
@Story(TRANSFORMERS)
public class TransformDiscoveryTestCase extends AbstractMuleContextTestCase {

  private final DefaultTransformersRegistry transformersRegistry = new DefaultTransformersRegistry();

  @Override
  protected void doSetUp() throws Exception {
    transformersRegistry.setTransformers(asList(new StringToApple(), new StringToOrange()));
    TypeBasedTransformerResolver transformerResolver = new TypeBasedTransformerResolver();
    transformerResolver.setTransformersRegistry(transformersRegistry);
    transformersRegistry.setTransformerResolvers(singletonList(transformerResolver));
    transformersRegistry.initialise();
  }

  @Test
  public void testSimpleDiscovery() throws Exception {
    Transformer t = transformersRegistry.lookupTransformer(DataType.STRING, DataType.fromType(Apple.class));
    assertThat(t, not(nullValue()));
    assertThat(t, instanceOf(StringToApple.class));

    t = transformersRegistry.lookupTransformer(DataType.STRING, DataType.fromType(Orange.class));
    assertThat(t, not(nullValue()));
    assertThat(t, instanceOf(StringToOrange.class));

    try {
      transformersRegistry.lookupTransformer(DataType.STRING, DataType.fromType(Banana.class));
      fail("There is no transformer to go from String to Banana");
    } catch (TransformerException e) {
      // expected
    }

    transformersRegistry.setTransformers(asList(new StringToApple(), new StringToOrange(), new StringToRedApple()));
    transformersRegistry.initialise();

    t = transformersRegistry.lookupTransformer(DataType.STRING, DataType.fromType(RedApple.class));
    assertThat(t, not(nullValue()));
    assertThat(t, instanceOf(StringToRedApple.class));
  }

  protected class StringToApple extends AbstractDiscoverableTransformer {

    public StringToApple() {
      setReturnDataType(DataType.fromType(Apple.class));
    }

    @Override
    protected Object doTransform(Object src, Charset encoding) throws TransformerException {
      return new Apple();
    }
  }

  protected class StringToRedApple extends AbstractDiscoverableTransformer {

    public StringToRedApple() {
      setReturnDataType(DataType.fromType(RedApple.class));
      setPriorityWeighting(MAX_PRIORITY_WEIGHTING);
    }

    @Override
    protected Object doTransform(Object src, Charset encoding) throws TransformerException {
      return new RedApple();
    }
  }

  protected class StringToOrange extends AbstractDiscoverableTransformer {

    public StringToOrange() {
      setReturnDataType(DataType.fromType(Orange.class));
    }

    @Override
    protected Object doTransform(Object src, Charset encoding) throws TransformerException {
      return new Orange();
    }
  }
}
