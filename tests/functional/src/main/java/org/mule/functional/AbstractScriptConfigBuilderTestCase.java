/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.tck.testmodels.mule.TestCompressionTransformer;

import org.junit.Test;

public abstract class AbstractScriptConfigBuilderTestCase extends FunctionalTestCase {

  // use legacy entry point resolver?
  private boolean legacy;

  protected AbstractScriptConfigBuilderTestCase() {
    this(false);
  }

  protected AbstractScriptConfigBuilderTestCase(boolean legacy) {
    this.legacy = legacy;
  }

  @Test
  public void testManagerConfig() throws Exception {
    assertEquals("true", muleContext.getRegistry().lookupObject("doCompression"));
    assertNotNull(muleContext.getTransactionManager());
  }

  @Test
  public void testTransformerConfig() {
    Transformer t = muleContext.getRegistry().lookupTransformer("TestCompressionTransformer");
    assertNotNull(t);
    assertTrue(t instanceof TestCompressionTransformer);
    assertEquals(t.getReturnDataType(), DataType.STRING);
    assertNotNull(((TestCompressionTransformer) t).getContainerProperty());
  }
}
