/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.AbstractTransformer;

import java.nio.charset.Charset;

import org.junit.Test;

public class TransformersInvokedFromResponseTestCase extends CompatibilityFunctionalTestCase {

  private static int counter1 = 0;

  @Override
  protected String getConfigFile() {
    return "transformers-invoked-from-response-config.xml";
  }

  @Test
  public void testTransformersAreCorrectlyInvoked() throws Exception {
    MuleClient client = muleContext.getClient();
    InternalMessage test = client.send("jms://testQueue", "TEST1", null).getRight();
    assertNotNull(test);
    assertEquals(1, counter1);
    assertEquals("TEST1 transformed", test.getPayload().getValue());

    test = client.send("jms://testQueue", "TEST2", null).getRight();
    assertNotNull(test);
    assertEquals(2, counter1);
    assertEquals("TEST2 transformed", test.getPayload().getValue());
  }

  public static class InvocationCounterTransformer1 extends AbstractTransformer {

    @Override
    protected Object doTransform(Object src, Charset enc) throws TransformerException {
      counter1++;
      return src;
    }
  }
}
