/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.source;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mule.test.heisenberg.extension.DEARadioSource.MESSAGES_PER_POLL;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.lifecycle.Callable;
import org.mule.runtime.core.api.util.concurrent.Latch;
import org.mule.test.heisenberg.extension.model.types.DEAOfficerAttributes;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ListOfMessagesSourceTestCase extends AbstractExtensionFunctionalTestCase implements Callable {

  private static AtomicReference<List<Message>> capturedPayload = new AtomicReference<>(null);
  private static Latch latch = new Latch();

  @Override
  protected String getConfigFile() {
    return "heisenberg-source-list-config.xml";
  }

  @Override
  protected void doTearDown() throws Exception {
    capturedPayload = null;
    latch = null;
    super.doTearDown();
  }

  @Override
  public Object onCall(MuleEventContext eventContext) throws Exception {
    List<Message> payload = (List<Message>) eventContext.getEvent().getMessage().getPayload().getValue();
    if (capturedPayload.compareAndSet(null, payload)) {
      latch.release();
    }
    return payload;
  }

  @Test
  public void listenMessages() throws Exception {
    assertThat(latch.await(5, SECONDS), is(true));
    List<Message> payload = capturedPayload.get();
    assertThat(payload, is(notNullValue()));
    assertThat(payload, hasSize(MESSAGES_PER_POLL));

    for (Message message : payload) {
      assertThat(message.getPayload().getValue(), is(instanceOf(String.class)));
      assertThat(message.getAttributes().getValue(), is(instanceOf(DEAOfficerAttributes.class)));
    }
  }
}
