/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.transformer;

import static java.lang.String.format;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.fail;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.internal.registry.TypeBasedTransformerResolver;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.Orange;

import java.nio.charset.Charset;

import org.junit.Test;

@SmallTest
public class TransformerResolutionTestCase extends AbstractMuleTestCase {

  public static final DataType FRUIT_DATA_TYPE = DataType.fromType(Fruit.class);
  public static final DataType ORANGE_DATA_TYPE = DataType.fromType(Orange.class);
  public static final DataType APPLE_DATA_TYPE = DataType.fromType(Apple.class);

  @Test
  public void resolvesMultipleApplicableTransformers() throws MuleException {
    DefaultTransformersRegistry registry = new DefaultTransformersRegistry();
    TypeBasedTransformerResolver resolver = new TypeBasedTransformerResolver();
    resolver.setTransformersRegistry(registry);
    registry.registerTransformerResolver(resolver);

    registry.registerTransformer(new StringToOrange());
    registry.registerTransformer(new StringToApple());
    registry.registerTransformer(new StringToFruit());

    try {
      Transformer transformer = registry.lookupTransformer(DataType.STRING, FRUIT_DATA_TYPE);
      assertThat(format("Expected a %s transformer but got %s", StringToFruit.class.getName(),
                        transformer.getClass().getName()),
                 transformer, instanceOf(StringToFruit.class));
    } catch (TransformerException e) {
      fail("Unable to properly resolve transformer");
    }
  }

  protected class AbstractStringToFruit extends AbstractDiscoverableTransformer {

    public AbstractStringToFruit() {
      registerSourceType(DataType.STRING);
    }

    @Override
    protected Object doTransform(Object src, Charset encoding) throws TransformerException {
      return new Orange();
    }
  }

  protected class StringToFruit extends AbstractStringToFruit {

    public StringToFruit() {
      setReturnDataType(FRUIT_DATA_TYPE);
    }
  }

  protected class StringToOrange extends AbstractStringToFruit {

    public StringToOrange() {
      setReturnDataType(ORANGE_DATA_TYPE);
    }
  }

  protected class StringToApple extends AbstractStringToFruit {

    public StringToApple() {
      setReturnDataType(APPLE_DATA_TYPE);
    }
  }
}
