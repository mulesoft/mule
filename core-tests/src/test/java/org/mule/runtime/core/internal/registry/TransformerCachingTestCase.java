/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.registry;

import static org.mule.runtime.api.metadata.DataType.BYTE_ARRAY;
import static org.mule.runtime.api.metadata.DataType.INPUT_STREAM;
import static org.mule.test.allure.AllureConstants.RegistryFeature.REGISTRY;
import static org.mule.test.allure.AllureConstants.RegistryFeature.TransfromersStory.TRANSFORMERS;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.AbstractTransformer;
import org.mule.runtime.core.api.transformer.DiscoverableTransformer;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.transformer.simple.InputStreamToByteArray;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.core.privileged.transformer.TransformersRegistry;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.FilterInputStream;
import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(REGISTRY)
@Story(TRANSFORMERS)
public class TransformerCachingTestCase extends AbstractMuleContextTestCase {

  private TransformersRegistry registry;

  @Before
  public void before() throws RegistrationException {
    registry = ((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(TransformersRegistry.class);
  }

  @Test
  public void testCacheUpdate() throws Exception {
    DataType sourceType = DataType.fromType(FilterInputStream.class);
    Transformer trans = registry.lookupTransformer(sourceType, BYTE_ARRAY);
    assertNotNull(trans);
    assertTrue(trans instanceof InputStreamToByteArray);

    Transformer trans2 = new FilterInputStreamToByteArray();
    registry.registerTransformer(trans2);

    trans = registry.lookupTransformer(sourceType, BYTE_ARRAY);
    assertNotNull(trans);
    assertTrue(trans instanceof FilterInputStreamToByteArray);

    trans = registry.lookupTransformer(INPUT_STREAM, BYTE_ARRAY);
    assertNotNull(trans);
    assertTrue(trans instanceof InputStreamToByteArray);
  }

  public static class FilterInputStreamToByteArray extends AbstractTransformer implements DiscoverableTransformer {

    public FilterInputStreamToByteArray() {
      registerSourceType(DataType.fromType(FilterInputStream.class));
      setReturnDataType(BYTE_ARRAY);
    }

    @Override
    protected Object doTransform(Object src, Charset outputEncoding) throws TransformerException {
      throw new UnsupportedOperationException("This is a transformer only to be used for testing");
    }

    @Override
    public int getPriorityWeighting() {
      return 0;
    }

    @Override
    public void setPriorityWeighting(int weighting) {
      // no-op
    }
  }
}
