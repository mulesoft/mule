/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.pgp;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.core.message.DefaultEventBuilder.EventImplementation.setCurrentEvent;

import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.transformer.encryption.EncryptionTransformer;
import org.mule.runtime.core.transformer.simple.ByteArrayToObject;
import org.mule.tck.testmodels.fruit.Orange;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class KBEStrategyUsingEncryptionTransformerTestCase extends AbstractEncryptionStrategyTestCase {

  @Test
  public void testEncrypt() throws Exception {
    String msg = "Test Message";
    FlowConstruct service = getTestFlowWithComponent("orange", Orange.class);

    Event event = Event.builder(DefaultEventContext.create(service, TEST_CONNECTOR))
        .message(InternalMessage.of(msg))
        .build();
    setCurrentEvent(event);

    EncryptionTransformer etrans = new EncryptionTransformer();
    etrans.setStrategy(kbStrategy);
    Object result = etrans.doTransform(msg.getBytes(), UTF_8);

    assertNotNull(result);
    InputStream inputStream = (InputStream) result;
    String message = IOUtils.toString(inputStream);
    String encrypted = (String) new ByteArrayToObject().doTransform(message.getBytes(), UTF_8);
    assertTrue(encrypted.startsWith("-----BEGIN PGP MESSAGE-----"));
  }
}
