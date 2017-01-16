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
import static org.mule.runtime.core.api.MessageExchangePattern.ONE_WAY;
import static org.mule.runtime.core.api.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.tck.MuleTestUtils.createErrorMock;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.runtime.core.message.DefaultExceptionPayload;
import org.mule.runtime.core.session.DefaultMuleSession;
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
    MuleSession session = new DefaultMuleSession();

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
    Processor intSetter = event -> {
      return Event.builder(event).message(InternalMessage.builder(event.getMessage()).payload(Integer.valueOf(1)).build())
          .build();
    };

    FirstSuccessful fs = createFirstSuccessfulRouter(intSetter, new StringAppendTransformer("abc"));
    fs.setFailureExpression("#[mel:payload is Integer]");
    fs.initialise();

    assertEquals("abc", fs.process(eventBuilder()
        .message(InternalMessage.of(""))
        .build()).getMessageAsString(muleContext));
  }

  @Test
  public void testRouteReturnsNullEvent() throws Exception {
    Processor nullReturningMp = event -> null;
    FirstSuccessful fs = createFirstSuccessfulRouter(nullReturningMp);
    fs.initialise();

    assertNull(fs.process(testEvent()));
  }

  @Test
  public void testRouteReturnsNullMessage() throws Exception {
    Processor nullEventMp = event -> Event.builder(event).message(null).build();
    FirstSuccessful fs = createFirstSuccessfulRouter(nullEventMp);
    fs.initialise();

    try {
      fs.process(testEvent());
      fail("Exception expected");
    } catch (CouldNotRouteOutboundMessageException e) {
      // this one was expected
    }
  }

  @Test
  public void testProcessingIsForcedOnSameThread() throws Exception {
    Processor checkForceSyncFlag = event -> {
      assertTrue(event.isSynchronous());
      return event;
    };
    FirstSuccessful router = createFirstSuccessfulRouter(checkForceSyncFlag);
    router.initialise();

    // the configured message processor will blow up if the router did not force processing
    // on same thread
    router.process(testEvent());
  }

  private FirstSuccessful createFirstSuccessfulRouter(Processor... processors) throws MuleException {
    FirstSuccessful fs = new FirstSuccessful();
    fs.setMuleContext(muleContext);

    List<Processor> routes = Arrays.asList(processors);
    fs.setRoutes(routes);

    return fs;
  }

  private String getPayload(Processor mp, MuleSession session, String message) throws Exception {
    InternalMessage msg = InternalMessage.builder().payload(message).build();
    try {
      Event event = mp.process(eventBuilder().message(msg).exchangePattern(REQUEST_RESPONSE).session(session).build());
      InternalMessage returnedMessage = event.getMessage();
      if (event.getError().isPresent()) {
        return EXCEPTION_SEEN;
      } else {
        return getPayloadAsString(returnedMessage);
      }
    } catch (Exception ex) {
      return EXCEPTION_SEEN;
    }
  }

  private static class TestProcessor implements Processor {

    private String rejectIfMatches;

    TestProcessor(String rejectIfMatches) {
      this.rejectIfMatches = rejectIfMatches;
    }

    @Override
    public Event process(Event event) throws MuleException {
      try {
        InternalMessage msg;
        Error error = null;
        String payload = event.getMessageAsString(muleContext);
        if (payload.indexOf(rejectIfMatches) >= 0) {
          throw new DefaultMuleException("Saw " + rejectIfMatches);
        } else if (payload.toLowerCase().indexOf(rejectIfMatches) >= 0) {
          Exception exception = new Exception();
          error = createErrorMock(exception);
          msg = InternalMessage.builder().nullPayload().exceptionPayload(new DefaultExceptionPayload(exception)).build();
        } else {
          msg = InternalMessage.builder().payload("No " + rejectIfMatches).build();
        }
        Event muleEvent = eventBuilder().message(msg).exchangePattern(ONE_WAY).error(error).build();
        return muleEvent;
      } catch (Exception e) {
        throw new DefaultMuleException(e);
      }
    }
  }
}
