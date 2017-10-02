/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.context.notification;

import static java.lang.System.lineSeparator;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.runtime.api.notification.EnrichedNotificationInfo.createInfo;
import static org.mule.runtime.api.notification.MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE;
import static org.mule.runtime.api.notification.PipelineMessageNotification.PROCESS_START;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.internal.context.notification.MessageProcessingFlowTraceManager.FLOW_STACK_INFO_KEY;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.notification.MessageProcessorNotification;
import org.mule.runtime.api.notification.PipelineMessageNotification;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.context.notification.ProcessorsTrace;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@SmallTest
public class MessageProcessingFlowTraceManagerTestCase extends AbstractMuleTestCase {

  public static final String CONFIG_FILE_NAME = "muleApp.xml";
  public static final int LINE_NUMBER = 10;
  private static QName docNameAttrName = new QName("http://www.mulesoft.org/schema/mule/documentation", "name");

  private static final String NESTED_FLOW_NAME = "nestedFlow";
  private static final String ROOT_FLOW_NAME = "rootFlow";
  private static final String APP_ID = "MessageProcessingFlowTraceManagerTestCase";

  private static boolean originalFlowTrace;

  @BeforeClass
  public static void beforeClass() {
    originalFlowTrace = DefaultMuleConfiguration.flowTrace;
    DefaultMuleConfiguration.flowTrace = true;
  }

  @AfterClass
  public static void afterClass() {
    DefaultMuleConfiguration.flowTrace = originalFlowTrace;
  }

  private MessageProcessingFlowTraceManager manager;
  private EventContext messageContext;

  private FlowConstruct rootFlowConstruct;
  private FlowConstruct nestedFlowConstruct;

  @Before
  public void before() {
    manager = new MessageProcessingFlowTraceManager();
    MuleContext context = mock(MuleContext.class);
    MuleConfiguration config = mock(MuleConfiguration.class);
    when(config.getId()).thenReturn(APP_ID);
    when(context.getConfiguration()).thenReturn(config);
    manager.setMuleContext(context);

    rootFlowConstruct = mock(FlowConstruct.class);
    ComponentLocation mockComponentLocation = mock(ComponentLocation.class);
    when(mockComponentLocation.getFileName()).thenReturn(of(CONFIG_FILE_NAME));
    when(mockComponentLocation.getLineInFile()).thenReturn(of(LINE_NUMBER));
    when(rootFlowConstruct.getLocation()).thenReturn(mockComponentLocation);
    when(rootFlowConstruct.getName()).thenReturn(ROOT_FLOW_NAME);
    when(rootFlowConstruct.getMuleContext()).thenReturn(context);
    nestedFlowConstruct = mock(FlowConstruct.class);
    when(nestedFlowConstruct.getLocation()).thenReturn(mockComponentLocation);
    when(nestedFlowConstruct.getName()).thenReturn(NESTED_FLOW_NAME);
    when(nestedFlowConstruct.getMuleContext()).thenReturn(context);

    messageContext = create(rootFlowConstruct, TEST_CONNECTOR_LOCATION);
  }

  @Test
  public void newFlowInvocation() {
    CoreEvent event = buildEvent("newFlowInvocation");
    PipelineMessageNotification pipelineNotification = buildPipelineNotification(event, rootFlowConstruct.getName());
    assertThat(getContextInfo(event, rootFlowConstruct), is(""));

    manager.onPipelineNotificationStart(pipelineNotification);
    assertThat(getContextInfo(event, rootFlowConstruct), is("at " + rootFlowConstruct.getName()));

    manager.onPipelineNotificationComplete(pipelineNotification);
    assertThat(getContextInfo(event, rootFlowConstruct), is(""));
  }

  @Test
  public void nestedFlowInvocations() {
    CoreEvent event = buildEvent("nestedFlowInvocations");
    PipelineMessageNotification pipelineNotification = buildPipelineNotification(event, rootFlowConstruct.getName());
    assertThat(getContextInfo(event, rootFlowConstruct), is(""));

    manager.onPipelineNotificationStart(pipelineNotification);
    manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(event,
                                                                               createMockProcessor(NESTED_FLOW_NAME + "_ref",
                                                                                                   false)));

    PipelineMessageNotification pipelineNotificationNested = buildPipelineNotification(event, NESTED_FLOW_NAME);
    manager.onPipelineNotificationStart(pipelineNotificationNested);

    String rootEntry = "at " + rootFlowConstruct.getName() + "(" + NESTED_FLOW_NAME + "_ref @ " + APP_ID + ":unknown:-1)";
    assertThat(getContextInfo(event, rootFlowConstruct), is("at " + NESTED_FLOW_NAME + lineSeparator() + rootEntry));

    manager.onPipelineNotificationComplete(pipelineNotificationNested);
    assertThat(getContextInfo(event, rootFlowConstruct), is(rootEntry));

    manager.onPipelineNotificationComplete(pipelineNotification);
    assertThat(getContextInfo(event, rootFlowConstruct), is(""));
  }

  @Test
  public void newComponentCall() {
    CoreEvent event = buildEvent("newComponentCall");
    PipelineMessageNotification pipelineNotification = buildPipelineNotification(event, rootFlowConstruct.getName());
    assertThat(getContextInfo(event, rootFlowConstruct), is(""));

    manager.onPipelineNotificationStart(pipelineNotification);
    assertThat(getContextInfo(event, rootFlowConstruct), is("at " + ROOT_FLOW_NAME));

    manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(event, createMockProcessor("/comp", false)));
    assertThat(getContextInfo(event, rootFlowConstruct), is("at " + ROOT_FLOW_NAME + "(/comp @ " + APP_ID + ":unknown:-1)"));

    manager.onPipelineNotificationComplete(pipelineNotification);
    assertThat(getContextInfo(event, rootFlowConstruct), is(""));
  }

  protected String getContextInfo(CoreEvent event, FlowConstruct flow) {
    return (String) manager.getContextInfo(createInfo(event, null, null), null).get(FLOW_STACK_INFO_KEY);
  }

  @Test
  public void newAnnotatedComponentCall() {
    CoreEvent event = buildEvent("newAnnotatedComponentCall");
    PipelineMessageNotification pipelineNotification = buildPipelineNotification(event, rootFlowConstruct.getName());
    assertThat(getContextInfo(event, rootFlowConstruct), is(""));

    manager.onPipelineNotificationStart(pipelineNotification);
    assertThat(getContextInfo(event, rootFlowConstruct), is("at " + rootFlowConstruct.getName()));

    Component annotatedMessageProcessor = (Component) createMockProcessor("/comp", true);

    when(annotatedMessageProcessor.getAnnotation(docNameAttrName)).thenReturn("annotatedName");
    manager
        .onMessageProcessorNotificationPreInvoke(buildProcessorNotification(event, (Processor) annotatedMessageProcessor));
    assertThat(getContextInfo(event, rootFlowConstruct),
               is("at " + rootFlowConstruct.getName() + "(/comp @ " + APP_ID + ":muleApp.xml:10 (annotatedName))"));

    manager.onPipelineNotificationComplete(pipelineNotification);
    assertThat(getContextInfo(event, rootFlowConstruct), is(""));
  }

  @Test
  public void splitStack() {
    CoreEvent event = buildEvent("newAnnotatedComponentCall");
    PipelineMessageNotification pipelineNotification = buildPipelineNotification(event, rootFlowConstruct.getName());
    assertThat(getContextInfo(event, rootFlowConstruct), is(""));

    manager.onPipelineNotificationStart(pipelineNotification);
    assertThat(getContextInfo(event, rootFlowConstruct), is("at " + rootFlowConstruct.getName()));

    manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(event, createMockProcessor("/comp", false)));
    assertThat(getContextInfo(event, rootFlowConstruct),
               is("at " + rootFlowConstruct.getName() + "(/comp @ " + APP_ID + ":unknown:-1)"));

    CoreEvent eventCopy = buildEvent("newAnnotatedComponentCall", event.getFlowCallStack().clone());
    assertThat(getContextInfo(eventCopy, rootFlowConstruct),
               is("at " + rootFlowConstruct.getName() + "(/comp @ " + APP_ID + ":unknown:-1)"));

    manager.onPipelineNotificationComplete(pipelineNotification);
    assertThat(getContextInfo(event, rootFlowConstruct), is(""));

    final FlowConstruct asyncFlowConstruct = mock(FlowConstruct.class);
    when(asyncFlowConstruct.getName()).thenReturn("asyncFlow");
    manager.onPipelineNotificationStart(buildPipelineNotification(eventCopy, asyncFlowConstruct.getName()));

    manager
        .onMessageProcessorNotificationPreInvoke(buildProcessorNotification(eventCopy, createMockProcessor("/asyncComp", false)));
    assertThat(getContextInfo(eventCopy, asyncFlowConstruct),
               is("at " + asyncFlowConstruct.getName() + "(/asyncComp @ " + APP_ID + ":unknown:-1)" + lineSeparator()
                   + "at " + rootFlowConstruct.getName() + "(/comp @ " + APP_ID + ":unknown:-1)"));
  }

  @Test
  public void splitStackEnd() {
    CoreEvent event = buildEvent("newAnnotatedComponentCall");
    PipelineMessageNotification pipelineNotification = buildPipelineNotification(event, rootFlowConstruct.getName());

    manager.onPipelineNotificationStart(pipelineNotification);
    manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(event, createMockProcessor("/comp", false)));
    FlowCallStack flowCallStack = event.getFlowCallStack();
    CoreEvent eventCopy = buildEvent("newAnnotatedComponentCall", flowCallStack.clone());
    manager.onPipelineNotificationComplete(pipelineNotification);
    String asyncFlowName = "asyncFlow";
    manager.onPipelineNotificationStart(buildPipelineNotification(eventCopy, asyncFlowName));
    manager
        .onMessageProcessorNotificationPreInvoke(buildProcessorNotification(eventCopy, createMockProcessor("/asyncComp", false)));

    assertThat(((BaseEventContext) event.getContext()).getProcessorsTrace(),
               hasExecutedProcessors("/comp @ " + APP_ID + ":unknown:-1",
                                     "/asyncComp @ " + APP_ID + ":unknown:-1"));
  }

  @Test
  public void joinStack() {
    CoreEvent event = buildEvent("joinStack");
    PipelineMessageNotification pipelineNotification = buildPipelineNotification(event, rootFlowConstruct.getName());
    assertThat(getContextInfo(event, rootFlowConstruct), is(""));

    manager.onPipelineNotificationStart(pipelineNotification);
    assertThat(getContextInfo(event, rootFlowConstruct), is("at " + rootFlowConstruct.getName()));

    manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(event,
                                                                               createMockProcessor("/scatter-gather", false)));
    assertThat(getContextInfo(event, rootFlowConstruct),
               is("at " + rootFlowConstruct.getName() + "(/scatter-gather @ " + APP_ID + ":unknown:-1)"));

    CoreEvent eventCopy0 = buildEvent("joinStack_0", event.getFlowCallStack().clone());
    CoreEvent eventCopy1 = buildEvent("joinStack_1", event.getFlowCallStack().clone());

    manager
        .onMessageProcessorNotificationPreInvoke(buildProcessorNotification(eventCopy0, createMockProcessor("/route_0", false)));

    manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(eventCopy1,
                                                                               createMockProcessor(NESTED_FLOW_NAME + "_ref",
                                                                                                   false)));
    PipelineMessageNotification pipelineNotificationNested = buildPipelineNotification(eventCopy1, NESTED_FLOW_NAME);
    manager.onPipelineNotificationStart(pipelineNotificationNested);
    manager
        .onMessageProcessorNotificationPreInvoke(buildProcessorNotification(eventCopy1, createMockProcessor("/route_1", false)));
    assertThat(getContextInfo(eventCopy1, rootFlowConstruct),
               is("at " + NESTED_FLOW_NAME + "(/route_1 @ " + APP_ID + ":unknown:-1)" + lineSeparator()
                   + "at " + ROOT_FLOW_NAME + "(" + NESTED_FLOW_NAME + "_ref @ " + APP_ID + ":unknown:-1)"));

    manager.onPipelineNotificationComplete(pipelineNotificationNested);

    assertThat(getContextInfo(event, rootFlowConstruct),
               is("at " + rootFlowConstruct.getName() + "(/scatter-gather @ " + APP_ID + ":unknown:-1)"));

    manager.onPipelineNotificationComplete(pipelineNotification);
    assertThat(getContextInfo(event, rootFlowConstruct), is(""));
  }

  public Processor createMockProcessor(String processorPath, boolean useLocationSettings) {
    ComponentLocation componentLocation = mock(ComponentLocation.class);
    when(componentLocation.getLocation()).thenReturn(processorPath);
    when(componentLocation.getFileName()).thenReturn(useLocationSettings ? of(CONFIG_FILE_NAME) : empty());
    when(componentLocation.getLineInFile()).thenReturn(useLocationSettings ? of(LINE_NUMBER) : empty());

    Component annotatedMessageProcessor =
        (Component) mock(Processor.class,
                         withSettings().extraInterfaces(Component.class).defaultAnswer(RETURNS_DEEP_STUBS));

    when(annotatedMessageProcessor.getAnnotation(any())).thenReturn(null);
    when(annotatedMessageProcessor.getLocation()).thenReturn(componentLocation);

    return (Processor) annotatedMessageProcessor;
  }

  @Test
  public void joinStackEnd() {
    CoreEvent event = buildEvent("joinStack");
    PipelineMessageNotification pipelineNotification = buildPipelineNotification(event, rootFlowConstruct.getName());

    manager.onPipelineNotificationStart(pipelineNotification);
    manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(event,
                                                                               createMockProcessor("/scatter-gather", false)));

    FlowCallStack flowCallStack = event.getFlowCallStack();
    CoreEvent eventCopy0 = buildEvent("joinStack_0", flowCallStack.clone());
    CoreEvent eventCopy1 = buildEvent("joinStack_1", flowCallStack.clone());

    manager
        .onMessageProcessorNotificationPreInvoke(buildProcessorNotification(eventCopy0, createMockProcessor("/route_0", false)));

    manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(eventCopy1,
                                                                               createMockProcessor(NESTED_FLOW_NAME + "_ref",
                                                                                                   false)));
    PipelineMessageNotification pipelineNotificationNested = buildPipelineNotification(eventCopy1, NESTED_FLOW_NAME);
    manager.onPipelineNotificationStart(pipelineNotificationNested);
    manager
        .onMessageProcessorNotificationPreInvoke(buildProcessorNotification(eventCopy1, createMockProcessor("/route_1", false)));
    manager.onPipelineNotificationComplete(pipelineNotificationNested);

    manager.onPipelineNotificationComplete(pipelineNotification);

    assertThat(((BaseEventContext) event.getContext()).getProcessorsTrace(),
               hasExecutedProcessors("/scatter-gather @ " + APP_ID + ":unknown:-1",
                                     "/route_0 @ " + APP_ID + ":unknown:-1",
                                     NESTED_FLOW_NAME + "_ref @ " + APP_ID + ":unknown:-1",
                                     "/route_1 @ " + APP_ID + ":unknown:-1"));
  }

  @Test
  public void mixedEvents() {
    CoreEvent event1 = buildEvent("mixedEvents_1");
    CoreEvent event2 = buildEvent("mixedEvents_2");

    PipelineMessageNotification pipelineNotification1 = buildPipelineNotification(event1, rootFlowConstruct.getName());
    PipelineMessageNotification pipelineNotification2 = buildPipelineNotification(event2, rootFlowConstruct.getName());
    assertThat(getContextInfo(event1, rootFlowConstruct), is(""));
    assertThat(getContextInfo(event2, rootFlowConstruct), is(""));

    manager.onPipelineNotificationStart(pipelineNotification1);
    assertThat(getContextInfo(event1, rootFlowConstruct), is("at " + rootFlowConstruct.getName()));
    assertThat(getContextInfo(event2, rootFlowConstruct), is(""));

    manager.onPipelineNotificationStart(pipelineNotification2);
    assertThat(getContextInfo(event1, rootFlowConstruct), is("at " + rootFlowConstruct.getName()));
    assertThat(getContextInfo(event2, rootFlowConstruct), is("at " + rootFlowConstruct.getName()));

    manager.onPipelineNotificationComplete(pipelineNotification1);
    assertThat(getContextInfo(event1, rootFlowConstruct), is(""));
    assertThat(getContextInfo(event2, rootFlowConstruct), is("at " + rootFlowConstruct.getName()));

    manager.onPipelineNotificationComplete(pipelineNotification2);
    assertThat(getContextInfo(event1, rootFlowConstruct), is(""));
    assertThat(getContextInfo(event2, rootFlowConstruct), is(""));
  }

  protected CoreEvent buildEvent(String eventId) {
    return buildEvent(eventId, new DefaultFlowCallStack());
  }

  protected CoreEvent buildEvent(String eventId, FlowCallStack flowStack) {
    CoreEvent event = mock(CoreEvent.class);
    when(event.getContext()).thenReturn(messageContext);
    when(event.getContext()).thenReturn(messageContext);
    when(event.getFlowCallStack()).thenReturn(flowStack);
    return event;
  }

  protected MessageProcessorNotification buildProcessorNotification(CoreEvent event, Processor processor) {
    return MessageProcessorNotification.createFrom(event, null, (Component) processor, null, MESSAGE_PROCESSOR_PRE_INVOKE);
  }

  protected PipelineMessageNotification buildPipelineNotification(CoreEvent event, String name) {
    Pipeline flowConstruct = mock(Pipeline.class, withSettings().extraInterfaces(Component.class));
    when(flowConstruct.getName()).thenReturn(name);

    return new PipelineMessageNotification(createInfo(event, null, flowConstruct), flowConstruct.getName(), PROCESS_START);
  }

  private Matcher<ProcessorsTrace> hasExecutedProcessors(final String... expectedProcessors) {
    return new TypeSafeMatcher<ProcessorsTrace>() {

      private List<Matcher> failed = new ArrayList<>();

      @Override
      protected boolean matchesSafely(ProcessorsTrace processorsTrace) {
        Matcher<Collection<? extends Object>> sizeMatcher = hasSize(expectedProcessors.length);
        if (!sizeMatcher.matches(processorsTrace.getExecutedProcessors())) {
          failed.add(sizeMatcher);
        }

        int i = 0;
        for (String expectedProcessor : expectedProcessors) {
          Matcher processorItemMatcher = is(expectedProcessor);
          if (!processorItemMatcher.matches(processorsTrace.getExecutedProcessors().get(i))) {
            failed.add(processorItemMatcher);
          }
          ++i;
        }

        return failed.isEmpty();
      }

      @Override
      public void describeTo(Description description) {
        description.appendValue(Arrays.asList(expectedProcessors));
      }

      @Override
      protected void describeMismatchSafely(ProcessorsTrace item, Description description) {
        description.appendText("was ").appendValue(item.getExecutedProcessors());
      }
    };
  }

}
