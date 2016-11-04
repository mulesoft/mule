/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.pgp;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.util.ExceptionUtils;
import org.mule.runtime.module.pgp.InvalidPublicKeyException;

import java.util.Optional;

import org.junit.Test;

public class PGPExpiredIntegrationTestCase extends MuleArtifactFunctionalTestCase {

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

    assertThat(client.request("test://out", 5000).getRight().isPresent(), is(false));

    Thread.sleep(2000);
    assertNotNull("flow's exception strategy should have caught an exception", exceptionFromFlow);
    InvalidPublicKeyException ipke = ExceptionUtils.getDeepestOccurenceOfType(exceptionFromFlow, InvalidPublicKeyException.class);
    assertNotNull("root cause must be a InvalidPublicKeyException", ipke);
    assertTrue(ipke.getMessage().contains("has expired"));
  }

  public static class ExceptionSaver implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      Optional<Error> error = event.getError();
      exceptionFromFlow = error.get().getCause();

      return null;
    }
  }
}
