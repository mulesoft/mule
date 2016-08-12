/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.runtime.core.DefaultMessageExecutionContext;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.runtime.core.message.DefaultExceptionPayload;
import org.mule.runtime.core.transformer.simple.StringAppendTransformer;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class FirstSuccessfulTestCase extends AbstractMuleContextTestCase {

  private static final String EXCEPTION_SEEN = "EXCEPTION WAS SEEN";

  public FirstSuccessfulTestCase() {
    setStartContext(true);
  }

  @Test
  public void testFirstSuccessful() throws Exception {
    MuleSession session = getTestSession(null, muleContext);

    FirstSuccessful fs =
        createFirstSuccessfulRouter(new TestProcessor("abc"), new TestProcessor("def"), new TestProcessor("ghi"));
    fs.initialise();

    assertEquals("No abc", getPayload(fs, session, ""));
    assertEquals("No def", getPayload(fs, session, "abc"));
    assertEquals("No ghi", getPayload(fs, session, "abcdef"));
    assertEquals(EXCEPTION_SEEN, getPayload(fs, session, "abcdefghi"));
    assertEquals("No def", getPayload(fs, session, "ABC"));
    assertEquals("No ghi", getPayload(fs, session, "ABCDEF"));
    assertEquals(EXCEPTION_SEEN, getPayload(fs, session, "ABCDEFGHI"));
  }

  @Test
  public void testFailureExpression() throws Exception {
    MessageProcessor intSetter = event -> {
      event.setMessage(MuleMessage.builder(event.getMessage()).payload(Integer.valueOf(1)).build());
      return event;
    };

    FirstSuccessful fs = createFirstSuccessfulRouter(intSetter, new StringAppendTransformer("abc"));
    fs.setFailureExpression("#[payload is Integer]");
    fs.initialise();

    assertEquals("abc", fs.process(getTestEvent("")).getMessageAsString());
  }

  @Test
  public void testRouteReturnsNullEvent() throws Exception {
    MessageProcessor nullReturningMp = event -> null;
    FirstSuccessful fs = createFirstSuccessfulRouter(nullReturningMp);
    fs.initialise();

    assertNull(fs.process(getTestEvent("")));
  }

  @Test
  public void testRouteReturnsNullMessage() throws Exception {
    MessageProcessor nullEventMp = event -> new DefaultMuleEvent(null, event);
    FirstSuccessful fs = createFirstSuccessfulRouter(nullEventMp);
    fs.initialise();

    try {
      fs.process(getTestEvent(""));
      fail("Exception expected");
    } catch (CouldNotRouteOutboundMessageException e) {
      // this one was expected
    }
  }

  @Test
  public void testProcessingIsForcedOnSameThread() throws Exception {
    MessageProcessor checkForceSyncFlag = event -> {
      assertTrue(event.isSynchronous());
      return event;
    };
    FirstSuccessful router = createFirstSuccessfulRouter(checkForceSyncFlag);
    router.initialise();

    // the configured message processor will blow up if the router did not force processing
    // on same thread
    router.process(getTestEvent(TEST_MESSAGE));
  }

  private FirstSuccessful createFirstSuccessfulRouter(MessageProcessor... processors) throws MuleException {
    FirstSuccessful fs = new FirstSuccessful();
    fs.setMuleContext(muleContext);

    List<MessageProcessor> routes = Arrays.asList(processors);
    fs.setRoutes(routes);

    return fs;
  }

  private String getPayload(MessageProcessor mp, MuleSession session, String message) throws Exception {
    MuleMessage msg = MuleMessage.builder().payload(message).build();
    try {
      MuleEvent event = mp.process(new DefaultMuleEvent(new DefaultMessageExecutionContext(muleContext.getUniqueIdString(), null),
                                                        msg, MessageExchangePattern.REQUEST_RESPONSE, getTestFlow(), session));
      MuleMessage returnedMessage = event.getMessage();
      if (returnedMessage.getExceptionPayload() != null) {
        System.out.println(returnedMessage.getExceptionPayload().getMessage());
        return EXCEPTION_SEEN;
      } else {
        return getPayloadAsString(returnedMessage);
      }
    } catch (Exception ex) {
      return EXCEPTION_SEEN;
    }
  }

  private static class TestProcessor implements MessageProcessor {

    private String rejectIfMatches;

    TestProcessor(String rejectIfMatches) {
      this.rejectIfMatches = rejectIfMatches;
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException {
      try {
        MuleMessage msg;
        String payload = event.getMessageAsString();
        if (payload.indexOf(rejectIfMatches) >= 0) {
          throw new DefaultMuleException("Saw " + rejectIfMatches);
        } else if (payload.toLowerCase().indexOf(rejectIfMatches) >= 0) {
          msg = MuleMessage.builder().nullPayload().exceptionPayload(new DefaultExceptionPayload(new Exception())).build();
        } else {
          msg = MuleMessage.builder().payload("No " + rejectIfMatches).build();
        }
        return new DefaultMuleEvent(new DefaultMessageExecutionContext(muleContext.getUniqueIdString(), null), msg,
                                    MessageExchangePattern.ONE_WAY, event.getFlowConstruct(), event.getSession());
      } catch (Exception e) {
        throw new DefaultMuleException(e);
      }
    }
  }
}
