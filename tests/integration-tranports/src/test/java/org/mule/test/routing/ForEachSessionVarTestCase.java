/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.processor.MessageProcessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;

public class ForEachSessionVarTestCase extends FunctionalTestCase {

  protected static MuleEvent event;
  private static final String MY_SESSION_LIST = "mySessionList";

  @Override
  protected String getConfigFile() {
    return "foreach-session-var-config.xml";
  }

  @Test
  public void testSessionVars() throws Exception {
    Collection<String> expectedArray = new ArrayList<>();
    expectedArray.add("Hello World A");
    expectedArray.add("Hello World B");

    flowRunner("test-foreachFlow1").withPayload(getTestMuleMessage()).run();

    // propierty should exist in the session and the message
    assertThat(event.getSession().<Collection<String>>getProperty(MY_SESSION_LIST), is(expectedArray));
    // removing the property from the session should affect the message as well
    event.getSession().removeProperty(MY_SESSION_LIST);
    assertThat(event.getSession().<Collection<String>>getProperty(MY_SESSION_LIST), is(nullValue()));
  }

  @Test
  public void counterConfiguration() throws Exception {
    final Collection<String> payload = new ArrayList<>();
    payload.add("wolfgang");
    payload.add("amadeus");
    payload.add("mozart");

    MuleMessage result = flowRunner("counter-config").withPayload(payload).run().getMessage();
    assertThat(result.getPayload(), instanceOf(Collection.class));
    Collection<?> resultPayload = (Collection<?>) result.getPayload();
    assertThat(resultPayload, hasSize(3));
    assertSame(payload, resultPayload);

    assertThat(result.getOutboundProperty("msg-last-index"), is(3));
  }

  @Test
  public void foreachWithAsync() throws Exception {
    final int size = 20;
    List<String> list = new ArrayList<>(size);

    for (int i = 0; i < size; i++) {
      list.add(RandomStringUtils.randomAlphabetic(10));
    }

    CountDownLatch latch = new CountDownLatch(size);
    flowRunner("foreachWithAsync").withPayload(list).withFlowVariable("latch", latch).run();

    latch.await(10, TimeUnit.SECONDS);
  }

  public static class EventSaverProcessor implements MessageProcessor {

    @Override
    public MuleEvent process(MuleEvent receivedEvent) throws MuleException {
      event = receivedEvent;
      return receivedEvent;
    }
  }

}
