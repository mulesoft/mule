/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.interception;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.interception.InterceptionAction;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.api.interception.InterceptionHandler;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.core.AbstractAnnotatedObject;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.source.polling.PollingMessageSource;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MessageProcessorInterceptionFlowTestCase extends AbstractIntegrationTestCase {

  private static final String INTERCEPTED = "intercepted";
  private static final String EXPECTED_INTERCEPTED_MESSAGE = TEST_MESSAGE + " " + INTERCEPTED;
  private static final String INPUT_MESSAGE = "inputMessage";

  private InterceptionHandlerHolder firstInterceptionHandlerHolder = new InterceptionHandlerHolder();
  private InterceptionHandlerHolder secondInterceptionHandlerHolder = new InterceptionHandlerHolder();

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/interception/interception-flow.xml";
  }

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    builders.add(new ConfigurationBuilder() {

      @Override
      public void configure(MuleContext muleContext) throws ConfigurationException {
        muleContext.getMessageProcessorInterceptorManager().addInterceptionHandler(firstInterceptionHandlerHolder);
        muleContext.getMessageProcessorInterceptorManager().addInterceptionHandler(secondInterceptionHandlerHolder);
      }

      @Override
      public boolean isConfigured() {
        return true;
      }
    });
    super.addBuilders(builders);
  }

  @Before
  public void before() {
    CountingProcessor.count.set(0);
  }

  @Test
  public void doNotIntercept() throws Exception {
    InterceptionHandler handler = new InterceptionHandler() {

      @Override
      public boolean intercept(ComponentIdentifier identifier, ComponentLocation location) {
        return false;
      }
    };
    firstInterceptionHandlerHolder.setHandler(handler);
    secondInterceptionHandlerHolder.setHandler(handler);
    final InternalMessage message = flowRunner("setPayloadFlow").withVariable(INPUT_MESSAGE, TEST_MESSAGE).run().getMessage();
    assertThat(message.getPayload().getValue(), equalTo(TEST_MESSAGE));
  }

  @Test
  public void interceptSkipOnFirstInterceptor() throws Exception {
    firstInterceptionHandlerHolder.setHandler(new InterceptionHandler() {

      @Override
      public void around(Map<String, Object> parameters, InterceptionEvent event, InterceptionAction action) {
        event.message(getInterceptedMessage(TEST_MESSAGE));
      }

      @Override
      public String toString() {
        return "interceptSkipOnFirstInterceptor-1";
      }
    });
    secondInterceptionHandlerHolder.setHandler(new InterceptionHandler() {

      @Override
      public void before(Map<String, Object> parameters, InterceptionEvent event) {
        fail("Should not be executed");
      }

      @Override
      public String toString() {
        return "interceptSkipOnFirstInterceptor-2";
      }
    });

    final InternalMessage message = flowRunner("setPayloadFlow").withVariable(INPUT_MESSAGE, "mock").run().getMessage();
    assertThat(message.getPayload().getValue(), equalTo(EXPECTED_INTERCEPTED_MESSAGE));
  }

  @Test
  public void interceptSkipOnSecondInterceptor() throws Exception {
    firstInterceptionHandlerHolder.setHandler(new InterceptionHandler() {

      @Override
      public void before(Map<String, Object> parameters, InterceptionEvent event) {
        event.message(getInterceptedMessage(event.getVariable(INPUT_MESSAGE).getValue()));
      }

    });
    secondInterceptionHandlerHolder.setHandler(new InterceptionHandler() {

      @Override
      public void around(Map<String, Object> parameters, InterceptionEvent event, InterceptionAction action) {}

    });

    final InternalMessage message = flowRunner("setPayloadFlow").withVariable(INPUT_MESSAGE, TEST_MESSAGE).run().getMessage();
    assertThat(message.getPayload().getValue(), equalTo(EXPECTED_INTERCEPTED_MESSAGE));
  }

  @Test
  public void interceptBothChangeEventAndContinue() throws Exception {
    firstInterceptionHandlerHolder.setHandler(new InterceptionHandler() {

      @Override
      public void before(java.util.Map<String, Object> parameters, InterceptionEvent event) {
        event.addVariable(INPUT_MESSAGE, TEST_MESSAGE + " " + INTERCEPTED);
      }

    });
    secondInterceptionHandlerHolder.setHandler(new InterceptionHandler() {

      @Override
      public void before(Map<String, Object> parameters, InterceptionEvent event) {
        event.addVariable(INPUT_MESSAGE, event.getVariable(INPUT_MESSAGE).getValue() + " " + INTERCEPTED);
      }

    });

    final InternalMessage message = flowRunner("setPayloadFlow").withVariable(INPUT_MESSAGE, "mock").run().getMessage();
    assertThat(message.getPayload().getValue(), equalTo(EXPECTED_INTERCEPTED_MESSAGE + " " + INTERCEPTED));
  }

  @Test
  public void interceptChangeInEventReflectedInHandlerParameters() throws Exception {
    firstInterceptionHandlerHolder.setHandler(new InterceptionHandler() {

      @Override
      public void before(java.util.Map<String, Object> parameters, InterceptionEvent event) {
        assertThat(parameters.get("value"), is("mock"));
        event.addVariable(INPUT_MESSAGE, "changed");
      }

    });
    secondInterceptionHandlerHolder.setHandler(new InterceptionHandler() {

      @Override
      public void before(Map<String, Object> parameters, InterceptionEvent event) {
        assertThat(parameters.get("value"), is("changed"));
        event.addVariable(INPUT_MESSAGE, TEST_MESSAGE);
      }

    });

    final InternalMessage message = flowRunner("setPayloadFlow").withVariable(INPUT_MESSAGE, "mock").run().getMessage();
    assertThat(message.getPayload().getValue(), equalTo(TEST_MESSAGE));
  }

  //TODO test exception in around!
  @Test
  public void interceptFirstInterceptorThrowsExceptionOnBefore() throws Exception {
    InterceptionHandler firstHandler = spy(new InterceptionHandler() {

      @Override
      public void before(Map<String, Object> parameters, InterceptionEvent event) {
        throw new MuleRuntimeException(createStaticMessage("Error"));
      }
    });
    firstInterceptionHandlerHolder.setHandler(firstHandler);
    InterceptionHandler secondHandler = spy(new NoOpInterceptionHandler());
    secondInterceptionHandlerHolder.setHandler(secondHandler);

    flowRunner("setPayloadFlow").withVariable(INPUT_MESSAGE, "mock").runExpectingException();

    verify(firstHandler).before(any(Map.class), any(InterceptionEvent.class));
    verify(firstHandler).after(any(InterceptionEvent.class));
    verify(secondHandler, never()).after(any(InterceptionEvent.class));
  }

  @Test
  public void interceptFirstInterceptorThrowsExceptionOnAfter() throws Exception {
    final MuleRuntimeException thrownException = new MuleRuntimeException(createStaticMessage("Error"));

    InterceptionHandler firstHandler = spy(new InterceptionHandler() {

      @Override
      public void after(InterceptionEvent event) {
        throw thrownException;
      }
    });
    firstInterceptionHandlerHolder.setHandler(firstHandler);
    InterceptionHandler secondHandler = spy(new NoOpInterceptionHandler());
    secondInterceptionHandlerHolder.setHandler(secondHandler);

    final MessagingException exception = flowRunner("setPayloadFlow").withVariable(INPUT_MESSAGE, "mock").runExpectingException();
    assertThat(exception.getCause().getCause(), sameInstance(thrownException));
  }

  @Test
  public void interceptSecondInterceptorThrowsExceptionOnAfter() throws Exception {
    final MuleRuntimeException thrownException = new MuleRuntimeException(createStaticMessage("Error"));

    InterceptionHandler firstHandler = spy(new NoOpInterceptionHandler());
    firstInterceptionHandlerHolder.setHandler(firstHandler);
    InterceptionHandler secondHandler = spy(new InterceptionHandler() {

      @Override
      public void after(InterceptionEvent event) {
        throw thrownException;
      }
    });
    secondInterceptionHandlerHolder.setHandler(secondHandler);

    final MessagingException exception = flowRunner("setPayloadFlow").withVariable(INPUT_MESSAGE, "mock").runExpectingException();
    assertThat(exception.getCause().getCause(), sameInstance(thrownException));

    verify(firstHandler, never()).after(any(InterceptionEvent.class));
  }

  @Test
  public void interceptedProcessorCalledJustOnce() throws Exception {
    flowRunner("countingProcessorFlow").run();

    assertThat(CountingProcessor.count.get(), is(1));
  }

  @Test
  public void internalProcessorNotIntercepted() throws Exception {
    InterceptionHandler firstHandler = spy(new InterceptionHandler() {

      @Override
      public boolean intercept(ComponentIdentifier identifier, ComponentLocation location) {
        fail("Internal Processors must not be intercepted");
        return false;
      }

      @Override
      public void before(Map<String, Object> parameters, InterceptionEvent event) {
        fail("Internal Processors must not be intercepted");
      }
    });
    firstInterceptionHandlerHolder.setHandler(firstHandler);
    InterceptionHandler secondHandler = spy(new NoOpInterceptionHandler());
    secondInterceptionHandlerHolder.setHandler(secondHandler);

    final InternalMessage message = flowRunner("internalProcessorFlow").run().getMessage();
    assertThat(message.getPayload().getValue(), is(TEST_MESSAGE));
  }

  //@Test
  //public void dynamicallyCreatedProcessorNotIntercepted() throws Exception {
  //  final Flow flow = new Flow("dynamicFlow", muleContext);
  //  flow.setMessageProcessors(singletonList(event -> event));
  //  muleContext.getRegistry().registerFlowConstruct(flow);
  //
  //  InterceptionHandler firstHandler = spy(new InterceptionHandler() {
  //
  //    @Override
  //    public boolean intercept(ComponentIdentifier identifier, ComponentLocation location) {
  //      fail("Internal Processors must not be intercepted");
  //      return false;
  //    }
  //
  //    @Override
  //    public void before(Map<String, Object> parameters, InterceptionEvent event) {
  //      fail("Internal Processors must not be intercepted");
  //    }
  //  });
  //  firstInterceptionHandlerHolder.setHandler(firstHandler);
  //  InterceptionHandler secondHandler = spy(new NoOpInterceptionHandler());
  //  secondInterceptionHandlerHolder.setHandler(secondHandler);
  //
  //  final InternalMessage message = flowRunner("dynamicFlow").run().getMessage();
  //}

  @Test
  public void routerIntercepted() throws Exception {
    InterceptionHandler handler = new InterceptionHandler() {

      @Override
      public boolean intercept(ComponentIdentifier identifier, ComponentLocation location) {
        return "choice".equals(identifier.getName());
      }

      @Override
      public void after(InterceptionEvent event) {
        assertThat(event.getMessage().getPayload().getValue(), equalTo(TEST_MESSAGE));
      }
    };
    firstInterceptionHandlerHolder.setHandler(handler);
    secondInterceptionHandlerHolder.setHandler(handler);
    final InternalMessage message = flowRunner("routerFlow").withVariable(INPUT_MESSAGE, TEST_MESSAGE).run().getMessage();
    assertThat(message.getPayload().getValue(), equalTo(TEST_MESSAGE));
  }

  //@Test
  //public void routerSkipNotAllowed() throws Exception {
  //  InterceptionHandler handler = new InterceptionHandler() {
  //
  //    @Override
  //    public boolean intercept(ComponentIdentifier identifier, ComponentLocation location) {
  //      return "choice".equals(identifier.getName());
  //    }
  //
  //    @Override
  //    public void before(Map<String, Object> parameters, InterceptionEvent event, InterceptionAction action) {
  //      // assertThat(action.isSkippable(), is(false));
  //      action.skip();
  //    }
  //  };
  //  firstInterceptionHandlerHolder.setHandler(handler);
  //  secondInterceptionHandlerHolder.setHandler(handler);
  //  flowRunner("routerFlow").withVariable(INPUT_MESSAGE, TEST_MESSAGE).runExpectingException();
  //}

  @Test
  @Ignore("MULE-11526: ComponentIdentifier/parameters not set into splitter/flow-ref components")
  public void flowRefIntercepted() throws Exception {
    InterceptionHandler handler = new InterceptionHandler() {

      @Override
      public boolean intercept(ComponentIdentifier identifier, ComponentLocation location) {
        return "flow-ref".equals(identifier.getName());
      }

      @Override
      public void after(InterceptionEvent event) {
        assertThat(event.getMessage().getPayload().getValue(), equalTo(TEST_MESSAGE));
      }
    };
    firstInterceptionHandlerHolder.setHandler(handler);
    secondInterceptionHandlerHolder.setHandler(handler);
    final InternalMessage message = flowRunner("flowWithInnerFlow").withVariable(INPUT_MESSAGE, TEST_MESSAGE).run().getMessage();
    assertThat(message.getPayload().getValue(), equalTo(TEST_MESSAGE));
  }

  @Test
  @Ignore("MULE-11526: ComponentIdentifier/parameters not set into splitter/flow-ref components")
  public void flowRefSkipNotAllowed() throws Exception {
    InterceptionHandler handler = new InterceptionHandler() {

      @Override
      public boolean intercept(ComponentIdentifier identifier, ComponentLocation location) {
        return "flow-ref".equals(identifier.getName());
      }

      @Override
      public void around(Map<String, Object> parameters, InterceptionEvent event, InterceptionAction action) {}

    };
    firstInterceptionHandlerHolder.setHandler(handler);
    secondInterceptionHandlerHolder.setHandler(handler);
    flowRunner("flowWithInnerFlow").withVariable(INPUT_MESSAGE, TEST_MESSAGE).runExpectingException();
  }

  @Test
  @Ignore("MULE-11526: ComponentIdentifier/parameters not set into splitter/flow-ref components")
  public void interceptiongProcessorIntercepted() throws Exception {
    InterceptionHandler handler = new InterceptionHandler() {

      @Override
      public boolean intercept(ComponentIdentifier identifier, ComponentLocation location) {
        // TODO ComponentIdentifier not being set for the splitter
        try {
          return identifier.getName() == null;
        } catch (NullPointerException e) {
          return true;
        }
      }

      @Override
      public void after(InterceptionEvent event) {
        assertThat(event.getMessage().getPayload().getValue(), equalTo(TEST_MESSAGE));
      }
    };
    firstInterceptionHandlerHolder.setHandler(handler);
    secondInterceptionHandlerHolder.setHandler(handler);
    final InternalMessage message =
        flowRunner("interceptingProcessorFlow").withPayload(TEST_MESSAGE).run().getMessage();
    assertThat(message.getPayload().getValue(), equalTo(TEST_MESSAGE));
  }

  @Test
  public void interceptiongProcessorSkipNotAllowed() throws Exception {
    InterceptionHandler handler = new InterceptionHandler() {

      @Override
      public boolean intercept(ComponentIdentifier identifier, ComponentLocation location) {
        // TODO ComponentIdentifier not being set for the splitter
        try {
          return identifier.getName() == null;
        } catch (NullPointerException e) {
          return true;
        }
      }

      @Override
      public void around(Map<String, Object> parameters, InterceptionEvent event, InterceptionAction action) {}
    };
    firstInterceptionHandlerHolder.setHandler(handler);
    secondInterceptionHandlerHolder.setHandler(handler);
    flowRunner("interceptingProcessorFlow").withVariable(INPUT_MESSAGE, TEST_MESSAGE).runExpectingException();
  }

  @Test
  @Ignore("MULE-11523: Interception is not being configured for Processor inside a poll")
  public void pollingSourceIntercepted() throws Exception {
    InterceptionHandler handler = spy(new InterceptionHandler() {

      @Override
      public void before(Map<String, Object> parameters, InterceptionEvent event) {
        event.addVariable(INPUT_MESSAGE, TEST_MESSAGE);
      }

      @Override
      public void after(InterceptionEvent event) {
        assertThat(event.getMessage().getPayload().getValue(), equalTo(TEST_MESSAGE));
      }
    });
    firstInterceptionHandlerHolder.setHandler(handler);
    secondInterceptionHandlerHolder.setHandler(handler);

    Flow flow = (Flow) (muleContext.getRegistry().lookupFlowConstruct("pollingFlow"));
    flow.start();
    try {
      MessageSource flowSource = flow.getMessageSource();
      if (flowSource instanceof PollingMessageSource) {
        ((PollingMessageSource) flowSource).performPoll();
      }

      new PollingProber().check(new JUnitLambdaProbe(() -> {
        verify(handler).before(any(Map.class), any(InterceptionEvent.class));
        verify(handler).after(any(InterceptionEvent.class));
        return true;
      }));
    } finally {
      flow.stop();
    }
  }

  private Message getInterceptedMessage(Object value) {
    return Message.builder().payload(value + " " + INTERCEPTED).build();
  }

  private class InterceptionHandlerHolder implements InterceptionHandler {

    private InterceptionHandler handler = new NoOpInterceptionHandler();

    public void setHandler(InterceptionHandler handler) {
      this.handler = handler;
    }

    @Override
    public boolean intercept(ComponentIdentifier identifier, ComponentLocation location) {
      return handler.intercept(identifier, location);
    }

    @Override
    public void before(Map<String, Object> parameters, InterceptionEvent event) {
      handler.before(parameters, event);
    }

    @Override
    public void around(Map<String, Object> parameters, InterceptionEvent event, InterceptionAction action) {
      handler.around(parameters, event, action);
    }

    @Override
    public void after(InterceptionEvent event) {
      handler.after(event);
    }

    @Override
    public String toString() {
      return handler.toString();
    }
  }

  private class NoOpInterceptionHandler implements InterceptionHandler {

    @Override
    public String toString() {
      return this.getClass().getName();
    }

  }

  public static class FailingProcessor extends AbstractAnnotatedObject implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      throw new DefaultMuleException("Expected");
    }
  }

  public static class CountingProcessor extends AbstractAnnotatedObject implements Processor {

    public static AtomicInteger count = new AtomicInteger(0);

    @Override
    public Event process(Event event) throws MuleException {
      count.incrementAndGet();
      return event;
    }
  }

  /**
   * What makes this internal is that this is not an {@link AnnotatedObject}.
   */
  public static class InternalProcessor implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      return Event.builder(event).message(Message.builder(event.getMessage()).payload(TEST_MESSAGE).build()).build();
    }
  }

}
