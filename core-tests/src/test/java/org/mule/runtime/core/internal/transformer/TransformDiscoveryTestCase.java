/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.tck.testmodels.fruit.RedApple;

import org.junit.Test;

import java.nio.charset.Charset;

public class TransformDiscoveryTestCase extends AbstractMuleContextTestCase {

  @Override
  protected void doSetUp() throws Exception {
    ((MuleContextWithRegistries) muleContext).getRegistry().registerTransformer(new StringToApple());
    ((MuleContextWithRegistries) muleContext).getRegistry().registerTransformer(new StringToOrange());
  }

  @Test
  public void testSimpleDiscovery() throws Exception {
    MuleRegistry registry = ((MuleContextWithRegistries) muleContext).getRegistry();
    Transformer t = registry.lookupTransformer(DataType.STRING, DataType.fromType(Apple.class));
    assertNotNull(t);
    assertEquals(StringToApple.class, t.getClass());

    t = registry.lookupTransformer(DataType.STRING, DataType.fromType(Orange.class));
    assertNotNull(t);
    assertEquals(StringToOrange.class, t.getClass());


    try {
      registry.lookupTransformer(DataType.STRING, DataType.fromType(Banana.class));
      fail("There is no transformer to go from String to Banana");
    } catch (TransformerException e) {
      // expected
    }


    registry.registerTransformer(new StringToRedApple());

    t = registry.lookupTransformer(DataType.STRING, DataType.fromType(RedApple.class));
    assertNotNull(t);
    assertEquals(StringToRedApple.class, t.getClass());
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
