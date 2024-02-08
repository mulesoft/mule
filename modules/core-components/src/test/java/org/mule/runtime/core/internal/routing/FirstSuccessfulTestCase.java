/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static org.mule.runtime.api.message.Message.of;
import static org.mule.tck.MuleTestUtils.createErrorMock;
import static org.mule.tck.processor.ContextPropagationChecker.assertContextPropagation;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ROUTERS;
import static org.mule.test.allure.AllureConstants.RoutersFeature.FirstSuccessfulStory.FIRST_SUCCESSFUL;

import static java.util.Arrays.asList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.processor.ContextPropagationChecker;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(ROUTERS)
@Story(FIRST_SUCCESSFUL)
public class FirstSuccessfulTestCase extends AbstractMuleContextTestCase {

  private static final String EXCEPTION_SEEN = "EXCEPTION WAS SEEN";

  @Rule
  public ExpectedException expectedException = none();

  public FirstSuccessfulTestCase() {
    setStartContext(true);
  }

  @Test
  public void testFirstSuccessful() throws Exception {
    FirstSuccessful fs =
        createFirstSuccessfulRouter(new TestProcessor("abc"), new TestProcessor("def"), new TestProcessor("ghi"));
    fs.initialise();

    assertThat(getPayload(fs, ""), is("No abc"));
    assertThat(getPayload(fs, "abc"), is("No def"));
    assertThat(getPayload(fs, "abcdef"), is("No ghi"));
    assertThat(getPayload(fs, "abcdefghi"), is(EXCEPTION_SEEN));
    assertThat(getPayload(fs, "ABC"), is("No def"));
    assertThat(getPayload(fs, "ABCDEF"), is("No ghi"));
    assertThat(getPayload(fs, "ABCDEFGHI"), is(EXCEPTION_SEEN));
  }

  @Test
  public void testRouteReturnsNullMessage() throws Exception {
    Processor nullEventMp = event -> CoreEvent.builder(event).message(c -> null).build();
    FirstSuccessful fs = createFirstSuccessfulRouter(nullEventMp);
    fs.setAnnotations(getAppleFlowComponentLocationAnnotations());
    fs.initialise();
    expectedException.expect(NullPointerException.class);
    fs.process(testEvent());
  }

  @Test
  public void subscriberContextPropagation() throws Exception {
    final ContextPropagationChecker contextPropagationChecker = new ContextPropagationChecker();

    FirstSuccessful fs = createFirstSuccessfulRouter(contextPropagationChecker);

    assertContextPropagation(testEvent(), fs, contextPropagationChecker);
  }

  private FirstSuccessful createFirstSuccessfulRouter(Processor... processors) throws Exception {
    FirstSuccessful fs = new FirstSuccessful();
    fs.setAnnotations(getAppleFlowComponentLocationAnnotations());
    final FlowConstruct flow = mock(FlowConstruct.class, withSettings().extraInterfaces(Component.class));
    when(flow.getMuleContext()).thenReturn(muleContext);
    when(((Component) flow).getLocation()).thenReturn(TEST_CONNECTOR_LOCATION);
    fs.setMuleContext(muleContext);
    muleContext.getInjector().inject(fs);
    fs.setRoutes(asList(processors));

    return fs;
  }

  private String getPayload(Processor mp, String message) throws Exception {
    Message msg = of(message);
    try {
      CoreEvent event = mp.process(this.<PrivilegedEvent.Builder>getEventBuilder().message(msg).build());
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

    private final String rejectIfMatches;

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
          msg = InternalMessage.builder().nullValue().build();
        } else {
          msg = of("No " + rejectIfMatches);
        }
        CoreEvent muleEvent = CoreEvent.builder(event).message(msg).error(error).build();
        return muleEvent;
      } catch (Exception e) {
        throw new DefaultMuleException(e);
      }
    }
  }
}
