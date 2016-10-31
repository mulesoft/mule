/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.vm.functional.transactions;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.connector.DispatchException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.transaction.TransactionException;
import org.mule.runtime.core.construct.Flow;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class VmSingleTransactionTransactionalElementTestCase extends CompatibilityFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "vm/vm-single-tx-transactional.xml";
  }

  @Before
  public void setUpTest() throws MuleException {
    purge("vm://out1?connector=vmConnector1");
    purge("vm://out2?connector=vmConnector1");
    purge("vm://out3?connector=vmConnector1");
  }

  private void purge(String endpoint) throws MuleException {
    while (muleContext.getClient().request(endpoint, 10).getRight().isPresent());
  }

  @Test
  public void testTransactional() throws Exception {
    Flow flow = (Flow) getFlowConstruct("transactional");
    Event event = Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
        .message(InternalMessage.of("message"))
        .build();
    flow.process(event);
    InternalMessage message1 = muleContext.getClient().request("vm://out1?connector=vmConnector1", 1000).getRight().get();
    InternalMessage message2 = muleContext.getClient().request("vm://out2?connector=vmConnector1", 1000).getRight().get();
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
    assertThat(muleContext.getClient().request("vm://out1?connector=vmConnector1", 1000).getRight().isPresent(), is(false));
    assertThat(muleContext.getClient().request("vm://out2?connector=vmConnector1", 1000).getRight().isPresent(), is(false));
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
    assertThat(muleContext.getClient().request("vm://out1?connector=vmConnector1", 1000).getRight().isPresent(), is(false));
    assertThat(muleContext.getClient().request("vm://out2?connector=vmConnector1", 1000).getRight().isPresent(), is(false));
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
    InternalMessage message1 = muleContext.getClient().request("vm://out1?connector=vmConnector1", 1000).getRight().get();
    InternalMessage message2 = muleContext.getClient().request("vm://out2?connector=vmConnector1", 1000).getRight().get();
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
    InternalMessage message1 = muleContext.getClient().request("vm://out1?connector=vmConnector1", 1000).getRight().get();
    assertThat(muleContext.getClient().request("vm://out2?connector=vmConnector1", 1000).getRight().isPresent(), is(false));
    assertThat(message1, notNullValue());
  }

  @Test
  public void testTransactionalFailAtEndWithCatchExceptionStrategy() throws Exception {
    Flow flow = (Flow) getFlowConstruct("transactionalFailAtEndWithCatchExceptionStrategy");
    Event event = Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
        .message(InternalMessage.of("message"))
        .build();
    flow.process(event);
    InternalMessage message1 = muleContext.getClient().request("vm://out1?connector=vmConnector1", 1000).getRight().get();
    InternalMessage message2 = muleContext.getClient().request("vm://out2?connector=vmConnector1", 1000).getRight().get();
    assertThat(message1, notNullValue());
    assertThat(message2, notNullValue());
  }

  @Test
  @Ignore
  public void testTransactionalWorksWithAnotherResourceType() throws Exception {
    Flow flow = (Flow) getFlowConstruct("transactionalWorksWithAnotherResourceType");
    Event event = Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
        .message(InternalMessage.of("message"))
        .build();
    try {
      flow.process(event);
      fail("DispatchException should be thrown");
    } catch (DispatchException e) {
      assertThat(e.getCause() instanceof TransactionException, is(true));
    }
    assertThat(muleContext.getClient().request("vm://out1?connector=vmConnector1", 1000).getRight().isPresent(), is(false));
    assertThat(muleContext.getClient().request("vm://out2?connector=vmConnector1", 1000).getRight().isPresent(), is(false));
  }

  @Test
  public void testTransactionalDoesntFailWithAnotherResourceType() throws Exception {
    Flow flow = (Flow) getFlowConstruct("transactionalDoesntFailWithAnotherResourceType");
    Event event = Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
        .message(InternalMessage.of("message"))
        .build();
    flow.process(event);
    InternalMessage message1 = muleContext.getClient().request("vm://out1?connector=vmConnector1", 1000).getRight().get();
    InternalMessage message2 = muleContext.getClient().request("vm://out2?connector=vmConnector1", 1000).getRight().get();
    InternalMessage message3 = muleContext.getClient().request("vm://out3?connector=vmConnector1", 1000).getRight().get();
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
    assertThat(muleContext.getClient().request("vm://out1?connector=vmConnector1", 1000).getRight().isPresent(), is(false));
    assertThat(muleContext.getClient().request("vm://out2?connector=vmConnector1", 1000).getRight().isPresent(), is(false));
    assertThat(muleContext.getClient().request("vm://out3?connector=vmConnector1", 1000).getRight().get(), notNullValue());
  }

  @Test
  public void testNestedTransactional() throws Exception {
    Flow flow = (Flow) getFlowConstruct("nestedTransactional");
    Event event = Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
        .message(InternalMessage.of("message"))
        .build();
    flow.process(event);
    InternalMessage message1 = muleContext.getClient().request("vm://out1?connector=vmConnector1", 1000).getRight().get();
    InternalMessage message2 = muleContext.getClient().request("vm://out2?connector=vmConnector1", 1000).getRight().get();
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
    InternalMessage message1 = muleContext.getClient().request("vm://out1?connector=vmConnector1", 1000).getRight().get();
    assertThat(muleContext.getClient().request("vm://out2?connector=vmConnector1", 1000).getRight().isPresent(), is(false));
    assertThat(message1, notNullValue());
  }

  @Test
  public void testNestedTransactionalFailWithCatch() throws Exception {
    Flow flow = (Flow) getFlowConstruct("nestedTransactionalFailWithCatch");
    Event event = Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
        .message(InternalMessage.of("message"))
        .build();
    flow.process(event);
    InternalMessage message1 = muleContext.getClient().request("vm://out1?connector=vmConnector1", 1000).getRight().get();
    InternalMessage message2 = muleContext.getClient().request("vm://out2?connector=vmConnector1", 1000).getRight().get();
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
    InternalMessage message1 = muleContext.getClient().request("vm://out1?connector=vmConnector1", 1000).getRight().get();
    InternalMessage message2 = muleContext.getClient().request("vm://out2?connector=vmConnector1", 1000).getRight().get();
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
    assertThat(muleContext.getClient().request("vm://out1?connector=vmConnector1", 1000).getRight().isPresent(), is(false));
    assertThat(muleContext.getClient().request("vm://out2?connector=vmConnector1", 1000).getRight().isPresent(), is(false));
  }

  @Test
  public void testNestedTransactionalWithBeginOrJoinFailWithCatch() throws Exception {
    Flow flow = (Flow) getFlowConstruct("nestedTransactionalWithBeginOrJoinFailWithCatch");
    Event event = Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR))
        .message(InternalMessage.of("message"))
        .build();
    flow.process(event);
    InternalMessage message1 = muleContext.getClient().request("vm://out1?connector=vmConnector1", 1000).getRight().get();
    InternalMessage message2 = muleContext.getClient().request("vm://out2?connector=vmConnector1", 1000).getRight().get();
    assertThat(message1, notNullValue());
    assertThat(message2, notNullValue());
  }

}
