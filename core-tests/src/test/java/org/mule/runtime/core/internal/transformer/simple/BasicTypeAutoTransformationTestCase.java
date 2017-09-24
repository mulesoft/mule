/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import static org.junit.Assert.assertEquals;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import java.math.BigDecimal;

public class BasicTypeAutoTransformationTestCase extends AbstractMuleContextTestCase {

  @Test
  public void testTypes() throws TransformerException {
    testType("1", Integer.class, Integer.TYPE, Integer.valueOf(1));
    testType("1", Long.class, Long.TYPE, Long.valueOf(1));
    testType("1", Short.class, Short.TYPE, Short.valueOf((short) 1));
    testType("1.1", Double.class, Double.TYPE, Double.valueOf(1.1));
    testType("1.1", Float.class, Float.TYPE, Float.valueOf((float) 1.1));
    testType("1.1", BigDecimal.class, null, BigDecimal.valueOf(1.1));
    testType("true", Boolean.class, Boolean.TYPE, Boolean.TRUE);
  }

  protected void testType(String string, Class type, Class primitive, Object value) throws TransformerException {
    assertEquals(value, lookupFromStringTransformer(type).transform(string));
    assertEquals(string, lookupToStringTransformer(type).transform(value));
    if (primitive != null) {
      assertEquals(value, lookupFromStringTransformer(primitive).transform(string));
      assertEquals(string, lookupToStringTransformer(primitive).transform(value));
    }
  }

  private Transformer lookupFromStringTransformer(Class to) throws TransformerException {
    return ((MuleContextWithRegistries) muleContext).getRegistry().lookupTransformer(DataType.STRING, DataType.fromType(to));
  }

  private Transformer lookupToStringTransformer(Class from) throws TransformerException {
    return ((MuleContextWithRegistries) muleContext).getRegistry().lookupTransformer(DataType.fromType(from), DataType.STRING);
  }

}
