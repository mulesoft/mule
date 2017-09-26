/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.tck.MuleTestUtils.createErrorMock;
import static org.mule.tck.util.MuleContextUtils.eventBuilder;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.message.DefaultExceptionPayload;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.privileged.event.DefaultMuleSession;
import org.mule.runtime.core.privileged.event.MuleSession;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.core.privileged.routing.CouldNotRouteOutboundMessageException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

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

    assertThat(getPayload(fs, session, ""), is("No abc"));
    assertThat(getPayload(fs, session, "abc"), is("No def"));
    assertThat(getPayload(fs, session, "abcdef"), is("No ghi"));
    assertThat(getPayload(fs, session, "abcdefghi"), is(EXCEPTION_SEEN));
    assertThat(getPayload(fs, session, "ABC"), is("No def"));
    assertThat(getPayload(fs, session, "ABCDEF"), is("No ghi"));
    assertThat(getPayload(fs, session, "ABCDEFGHI"), is(EXCEPTION_SEEN));
  }

  @Test
  public void testRouteReturnsNullEvent() throws Exception {
    Processor nullReturningMp = event -> null;
    FirstSuccessful fs = createFirstSuccessfulRouter(nullReturningMp);
    fs.setAnnotations(getAppleFlowComponentLocationAnnotations());
    fs.initialise();

    assertThat(fs.process(testEvent()), nullValue());
  }

  @Test
  public void testRouteReturnsNullMessage() throws Exception {
    Processor nullEventMp = event -> CoreEvent.builder(event).message(null).build();
    FirstSuccessful fs = createFirstSuccessfulRouter(nullEventMp);
    fs.setAnnotations(getAppleFlowComponentLocationAnnotations());
    fs.initialise();

    try {
      fs.process(testEvent());
      fail("Exception expected");
    } catch (CouldNotRouteOutboundMessageException e) {
      // this one was expected
    }
  }

  private FirstSuccessful createFirstSuccessfulRouter(Processor... processors) throws Exception {
    FirstSuccessful fs = new FirstSuccessful();
    fs.setAnnotations(getAppleFlowComponentLocationAnnotations());
    final FlowConstruct flow = mock(FlowConstruct.class, withSettings().extraInterfaces(Component.class));
    when(flow.getMuleContext()).thenReturn(muleContext);
    when(((Component) flow).getLocation()).thenReturn(TEST_CONNECTOR_LOCATION);
    fs.setMuleContext(muleContext);

    fs.setRoutes(asList(processors));

    return fs;
  }

  private String getPayload(Processor mp, MuleSession session, String message) throws Exception {
    Message msg = of(message);
    try {
      CoreEvent event = mp.process(this.<PrivilegedEvent.Builder>getEventBuilder().message(msg).session(session).build());
      Message returnedMessage = event.getMessage();
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
    public CoreEvent process(CoreEvent event) throws MuleException {
      try {
        Message msg;
        Error error = null;
        String payload = ((InternalEvent) event).getMessageAsString(muleContext);
        if (payload.indexOf(rejectIfMatches) >= 0) {
          throw new DefaultMuleException("Saw " + rejectIfMatches);
        } else if (payload.toLowerCase().indexOf(rejectIfMatches) >= 0) {
          Exception exception = new Exception();
          error = createErrorMock(exception);
          msg = InternalMessage.builder().nullValue().exceptionPayload(new DefaultExceptionPayload(exception)).build();
        } else {
          msg = of("No " + rejectIfMatches);
        }
        CoreEvent muleEvent = eventBuilder(muleContext).message(msg).error(error).build();
        return muleEvent;
      } catch (Exception e) {
        throw new DefaultMuleException(e);
      }
    }
  }
}
