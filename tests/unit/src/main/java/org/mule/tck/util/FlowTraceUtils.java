/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.tck.util;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.context.notification.FlowStackElement;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.event.EventContextService;
import org.mule.runtime.core.api.processor.Processor;

import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class FlowTraceUtils {

  public static class FlowStackAsserter implements Processor, Disposable {

    @Inject
    private EventContextService eventContextService;

    public static FlowCallStack stackToAssert;

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      eventContextService.getCurrentlyActiveFlowStacks().stream()
          .filter(fsde -> fsde.getEventId().equals(event.getContext().getId())).findAny()
          .ifPresent(dumpEntry -> stackToAssert = dumpEntry.getFlowCallStack().clone());

      return event;
    }

    @Override
    public void dispose() {
      stackToAssert = null;
    }
  }

  public static class FlowStackAsyncAsserter extends FlowStackAsserter {

    public static CountDownLatch latch;

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      super.process(event);
      latch.countDown();
      return event;
    }
  }

  @SafeVarargs
  public static void assertStackElements(FlowCallStack flowStack, Matcher<FlowStackElement>... flowStackElementMatchers) {
    assertThat(flowStack.toString(), flowStack.getElements(), hasSize(flowStackElementMatchers.length));
    int i = 0;
    for (Matcher<FlowStackElement> flowStackElementMatcher : flowStackElementMatchers) {
      assertThat(flowStack.toString(), flowStack.getElements().get(i), flowStackElementMatcher);
      ++i;
    }
  }

  public static Matcher<FlowStackElement> isFlowStackElement(final String flowName, final String executingMessageProcessor) {
    return new TypeSafeMatcher<FlowStackElement>() {

      @Override
      protected boolean matchesSafely(FlowStackElement flowStackElement) {
        return flowStackElement.getFlowName().startsWith(flowName)
            && (executingMessageProcessor == null ? flowStackElement.getProcessorPath() == null
                : flowStackElement.getProcessorPath().startsWith(executingMessageProcessor + " @"));
      }

      @Override
      public void describeTo(Description description) {
        if (executingMessageProcessor == null) {
          description.appendText("<").appendText(flowName);
        } else {
          description.appendText("<").appendText(flowName).appendText("(").appendText(executingMessageProcessor).appendText("*)");
        }
      }
    };
  }

  public static Matcher<FlowStackElement> withChainIdentifier(final ComponentIdentifier chainIdentifier) {
    return new TypeSafeMatcher<FlowStackElement>() {

      @Override
      protected boolean matchesSafely(FlowStackElement flowStackElement) {
        return flowStackElement.getChainIdentifier().equals(chainIdentifier);
      }

      @Override
      public void describeTo(Description description) {
        description
            .appendText("identifier with namespace ")
            .appendValue(chainIdentifier.getNamespace())
            .appendText(" and name ")
            .appendValue(chainIdentifier.getName());
      }

      @Override
      protected void describeMismatchSafely(FlowStackElement flowStackElement, Description mismatchDescription) {
        mismatchDescription
            .appendText("identifier with namespace ")
            .appendValue(flowStackElement.getChainIdentifier().getNamespace())
            .appendText(" and name ")
            .appendValue(flowStackElement.getChainIdentifier().getName());
      }
    };
  }
}
