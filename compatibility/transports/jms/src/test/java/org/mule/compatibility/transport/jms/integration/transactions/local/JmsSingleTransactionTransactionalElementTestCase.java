/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.jms.integration.transactions.local;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.functional.junit4.matchers.ThrowableCauseMatcher.hasCause;

import org.mule.compatibility.transport.jms.integration.AbstractJmsFunctionalTestCase;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.connector.DispatchException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.exception.MessagingException;

import javax.jms.JMSException;

import org.junit.Before;
import org.junit.Test;

public class JmsSingleTransactionTransactionalElementTestCase extends AbstractJmsFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "integration/transactions/local/jms-single-tx-transactional.xml";
  }

  @Before
  public void setUpTest() throws JMSException {
    purge("out1");
    purge("out2");
    purge("out3");
  }

  @Test
  public void testTransactional() throws Exception {
    Flow flow = (Flow) getFlowConstruct("transactional");
    Event event = Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
        .message(InternalMessage.of("message"))
        .build();
    flow.process(event);
    InternalMessage message1 = muleContext.getClient().request("out1", 1000).getRight().get();
    InternalMessage message2 = muleContext.getClient().request("out2", 1000).getRight().get();
    assertThat(message1, notNullValue());
    assertThat(message2, notNullValue());
  }

  @Test
  public void testTransactionalFailInTheMiddle() throws Exception {
    Flow flow = (Flow) getFlowConstruct("transactionalFailInTheMiddle");
    Event event = Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
        .message(InternalMessage.of("message"))
        .build();
    try {
      flow.process(event);
    } catch (Exception e) {
    }
    assertThat(muleContext.getClient().request("out1", 1000).getRight().isPresent(), is(false));
    assertThat(muleContext.getClient().request("out2", 1000).getRight().isPresent(), is(false));
  }

  @Test
  public void testTransactionalFailAtEnd() throws Exception {
    Flow flow = (Flow) getFlowConstruct("transactionalFailAtEnd");
    Event event = Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
        .message(InternalMessage.of("message"))
        .build();
    try {
      flow.process(event);
    } catch (Exception e) {
    }
    assertThat(muleContext.getClient().request("out1", 1000).getRight().isPresent(), is(false));
    assertThat(muleContext.getClient().request("out2", 1000).getRight().isPresent(), is(false));
  }

  @Test
  public void testTransactionalFailAfterEnd() throws Exception {
    Flow flow = (Flow) getFlowConstruct("transactionalFailAfterEnd");
    Event event = Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
        .message(InternalMessage.of("message"))
        .build();
    try {
      flow.process(event);
    } catch (Exception e) {
    }
    InternalMessage message1 = muleContext.getClient().request("out1", 1000).getRight().get();
    InternalMessage message2 = muleContext.getClient().request("out2", 1000).getRight().get();
    assertThat(message1, notNullValue());
    assertThat(message2, notNullValue());
  }

  @Test
  public void testTransactionalFailInTheMiddleWithCatchExceptionStrategy() throws Exception {
    Flow flow = (Flow) getFlowConstruct("transactionalFailInTheMiddleWithCatchExceptionStrategy");
    Event event = Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
        .message(InternalMessage.of("message"))
        .build();
    flow.process(event);
    InternalMessage message1 = muleContext.getClient().request("out1", 1000).getRight().get();
    assertThat(muleContext.getClient().request("out2", 1000).getRight().isPresent(), is(false));
    assertThat(message1, notNullValue());
  }

  @Test
  public void testTransactionalFailAtEndWithCatchExceptionStrategy() throws Exception {
    Flow flow = (Flow) getFlowConstruct("transactionalFailAtEndWithCatchExceptionStrategy");
    Event event = Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
        .message(InternalMessage.of("message"))
        .build();
    flow.process(event);
    InternalMessage message1 = muleContext.getClient().request("out1", 1000).getRight().get();
    InternalMessage message2 = muleContext.getClient().request("out2", 1000).getRight().get();
    assertThat(message1, notNullValue());
    assertThat(message2, notNullValue());
  }

  @Test
  public void testTransactionalFailsWithAnotherResourceType() throws Exception {
    Flow flow = (Flow) getFlowConstruct("transactionalFailsWithAnotherResourceType");
    Event event = Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
        .message(InternalMessage.of("message"))
        .build();
    try {
      flow.process(event);
      fail("DispatchException should be thrown");
    } catch (MessagingException e) {
      assertThat(e, hasCause(instanceOf(DispatchException.class)));
    }
    assertThat(muleContext.getClient().request("out1", 1000).getRight().isPresent(), is(false));
    assertThat(muleContext.getClient().request("out2", 1000).getRight().isPresent(), is(false));
  }

  @Test
  public void testTransactionalDoesntFailWithAnotherResourceType() throws Exception {
    Flow flow = (Flow) getFlowConstruct("transactionalDoesntFailWithAnotherResourceType");
    Event event = Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
        .message(InternalMessage.of("message"))
        .build();
    flow.process(event);
    InternalMessage message1 = muleContext.getClient().request("out1", 1000).getRight().get();
    InternalMessage message2 = muleContext.getClient().request("out2", 1000).getRight().get();
    InternalMessage message3 = muleContext.getClient().request("out3", 1000).getRight().get();
    assertThat(message1, notNullValue());
    assertThat(message2, notNullValue());
    assertThat(message3, notNullValue());
  }

  @Test
  public void testTransactionalWithAnotherResourceTypeAndExceptionAtEnd() throws Exception {
    Flow flow = (Flow) getFlowConstruct("transactionalWithAnotherResourceTypeAndExceptionAtEnd");
    Event event = Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
        .message(InternalMessage.of("message"))
        .build();
    try {
      flow.process(event);
    } catch (Exception e) {
    }
    assertThat(muleContext.getClient().request("out1", 1000).getRight().isPresent(), is(false));
    assertThat(muleContext.getClient().request("out2", 1000).getRight().isPresent(), is(false));
    InternalMessage message3 = muleContext.getClient().request("out3", 1000).getRight().get();
    assertThat(message3, notNullValue());
  }

  @Test
  public void testNestedTransactional() throws Exception {
    Flow flow = (Flow) getFlowConstruct("nestedTransactional");
    Event event = Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
        .message(InternalMessage.of("message"))
        .build();
    flow.process(event);
    InternalMessage message1 = muleContext.getClient().request("out1", 1000).getRight().get();
    InternalMessage message2 = muleContext.getClient().request("out2", 1000).getRight().get();
    assertThat(message1, notNullValue());
    assertThat(message2, notNullValue());
  }

  @Test
  public void testNestedTransactionalFail() throws Exception {
    Flow flow = (Flow) getFlowConstruct("nestedTransactionalFail");
    Event event = Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
        .message(InternalMessage.of("message"))
        .build();
    try {
      flow.process(event);
    } catch (Exception e) {
    }
    InternalMessage message1 = muleContext.getClient().request("out1", 1000).getRight().get();
    assertThat(muleContext.getClient().request("out2", 1000).getRight().isPresent(), is(false));
    assertThat(message1, notNullValue());
  }

  @Test
  public void testNestedTransactionalFailWithCatch() throws Exception {
    Flow flow = (Flow) getFlowConstruct("nestedTransactionalFailWithCatch");
    Event event = Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
        .message(InternalMessage.of("message"))
        .build();
    flow.process(event);
    InternalMessage message1 = muleContext.getClient().request("out1", 1000).getRight().get();
    InternalMessage message2 = muleContext.getClient().request("out2", 1000).getRight().get();
    assertThat(message1, notNullValue());
    assertThat(message2, notNullValue());
  }



  @Test
  public void testNestedTransactionalWithBeginOrJoin() throws Exception {
    Flow flow = (Flow) getFlowConstruct("nestedTransactionalWithBeginOrJoin");
    Event event = Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
        .message(InternalMessage.of("message"))
        .build();
    flow.process(event);
    InternalMessage message1 = muleContext.getClient().request("out1", 1000).getRight().get();
    InternalMessage message2 = muleContext.getClient().request("out2", 1000).getRight().get();
    assertThat(message1, notNullValue());
    assertThat(message2, notNullValue());
  }

  @Test
  public void testNestedTransactionalWithBeginOrJoinFail() throws Exception {
    Flow flow = (Flow) getFlowConstruct("nestedTransactionalWithBeginOrJoinFail");
    Event event = Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
        .message(InternalMessage.of("message"))
        .build();
    try {
      flow.process(event);
    } catch (Exception e) {
    }
    assertThat(muleContext.getClient().request("out1", 1000).getRight().isPresent(), is(false));
    assertThat(muleContext.getClient().request("out2", 1000).getRight().isPresent(), is(false));
  }

  @Test
  public void testNestedTransactionalWithBeginOrJoinFailWithCatch() throws Exception {
    Flow flow = (Flow) getFlowConstruct("nestedTransactionalWithBeginOrJoinFailWithCatch");
    Event event = Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
        .message(InternalMessage.of("message"))
        .build();
    flow.process(event);
    InternalMessage message1 = muleContext.getClient().request("out1", 1000).getRight().get();
    InternalMessage message2 = muleContext.getClient().request("out2", 1000).getRight().get();
    assertThat(message1, notNullValue());
    assertThat(message2, notNullValue());
  }

  @Test
  public void testNestedTransactionalWithBeginOrJoinFailWithCatchAndRollback() throws Exception {
    Flow flow = (Flow) getFlowConstruct("nestedTransactionalWithBeginOrJoinFailWithCatchAndRollback");
    Event event = Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
        .message(InternalMessage.of("message"))
        .build();
    flow.process(event);
    assertThat(muleContext.getClient().request("out1", 1000).getRight().isPresent(), is(false));
    assertThat(muleContext.getClient().request("out2", 1000).getRight().isPresent(), is(false));
  }

}
