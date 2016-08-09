/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.pgp;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.mule.runtime.core.api.ExceptionPayload;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.util.ExceptionUtils;

import org.junit.Test;

public class PGPExpiredIntegrationTestCase extends FunctionalTestCase {

  private static Throwable exceptionFromFlow = null;

  @Override
  protected String getConfigFile() {
    return "pgp-expired-integration-mule-config-flow.xml";
  }

  @Test
  public void testEncryptDecrypt() throws Exception {
    String payload = "this is a super simple test. Hope it works!!!";
    MuleClient client = muleContext.getClient();

    flowRunner("pgpEncryptProcessor").withPayload(payload).asynchronously().run();

    MuleMessage message = client.request("test://out", 5000);
    assertNull(message);

    assertNotNull("flow's exception strategy should have caught an exception", exceptionFromFlow);
    InvalidPublicKeyException ipke = ExceptionUtils.getDeepestOccurenceOfType(exceptionFromFlow, InvalidPublicKeyException.class);
    assertNotNull("root cause must be a InvalidPublicKeyException", ipke);
    assertTrue(ipke.getMessage().contains("has expired"));
  }

  public static class ExceptionSaver implements MessageProcessor {

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException {
      ExceptionPayload exceptionPayload = event.getMessage().getExceptionPayload();
      exceptionFromFlow = exceptionPayload.getException();

      return null;
    }
  }
}
