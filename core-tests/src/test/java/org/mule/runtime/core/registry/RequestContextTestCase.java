/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.registry;

import static java.time.OffsetTime.now;
import static org.junit.Assert.assertEquals;
import static org.mule.runtime.core.DefaultMuleEvent.setCurrentEvent;
import static org.mule.runtime.core.message.ErrorBuilder.builder;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MessageContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.context.notification.ProcessorsTrace;
import org.mule.runtime.core.api.security.SecurityContext;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.message.Correlation;
import org.mule.runtime.core.message.ErrorBuilder;
import org.mule.runtime.core.message.DefaultExceptionPayload;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.nio.charset.Charset;
import java.time.OffsetTime;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

public class RequestContextTestCase extends AbstractMuleTestCase {

  private boolean threadSafeEvent;

  @Test
  public void testSetExceptionPayloadAcrossThreads() throws InterruptedException {
    threadSafeEvent = true;
    MuleEvent event = new DummyEvent();
    runThread(event, false);
    runThread(event, true);
  }

  @Test
  public void testFailureWithoutThreadSafeEvent() throws InterruptedException {
    threadSafeEvent = false;
    MuleEvent event = new DummyEvent();
    runThread(event, false);
    runThread(event, true);
  }

  protected void runThread(MuleEvent event, boolean doTest) throws InterruptedException {
    AtomicBoolean success = new AtomicBoolean(false);
    Thread thread = new Thread(new SetExceptionPayload(event, success));
    thread.start();
    thread.join();
    if (doTest) {
      // Since events are now immutable, there should be no failures due to this!
      assertEquals(true, success.get());
    }
  }

  private class SetExceptionPayload implements Runnable {

    private MuleEvent event;
    private AtomicBoolean success;

    public SetExceptionPayload(MuleEvent event, AtomicBoolean success) {
      this.event = event;
      this.success = success;
    }

    @Override
    public void run() {
      try {
        Exception exception = new Exception();
        event.setMessage(MuleMessage.builder(event.getMessage()).exceptionPayload(new DefaultExceptionPayload(exception))
            .build());
        event.setError(builder(exception).build());
        setCurrentEvent(event);
        success.set(true);
      } catch (RuntimeException e) {
        logger.error("error in thread", e);
      }
    }

  }

  private class DummyEvent implements MuleEvent {

    private MuleMessage message = MuleMessage.builder().payload("").build();

    @Override
    public MessageContext getContext() {
      return new MessageContext() {

        @Override
        public String getId() {
          return "";
        }

        @Override
        public String getCorrelationId() {
          return "";
        }

        @Override
        public OffsetTime getReceivedTime() {
          return now();
        }

        @Override
        public String getOriginatingFlowName() {
          return "";
        }

        @Override
        public String getOriginatingConnectorName() {
          return "test";
        }

        @Override
        public boolean isCorrelationIdFromSource() {
          return false;
        }
      };
    }

    @Override
    public MuleMessage getMessage() {
      return message;
    }

    @Override
    public Error getError() {
      return null;
    }

    public byte[] getMessageAsBytes(MuleContext muleContext) throws MuleException {
      return new byte[0];
    }

    @Override
    public Object transformMessage(Class outputType, MuleContext muleContext) throws TransformerException {
      return null;
    }

    @Override
    public String transformMessageToString(MuleContext muleContext) throws TransformerException {
      return null;
    }

    @Override
    public String getMessageAsString(MuleContext muleContext) throws MuleException {
      return null;
    }

    @Override
    public Object transformMessage(DataType outputType, MuleContext muleContext) throws TransformerException {
      return null;
    }

    @Override
    public String getMessageAsString(Charset encoding, MuleContext muleContext) throws MuleException {
      return null;
    }

    @Override
    public String getId() {
      return null;
    }

    @Override
    public MuleSession getSession() {
      return null;
    }

    @Override
    public FlowConstruct getFlowConstruct() {
      return null;
    }

    @Override
    public MuleContext getMuleContext() {
      return null;
    }

    @Override
    public MessageExchangePattern getExchangePattern() {
      return null;
    }

    @Override
    public boolean isTransacted() {
      return false;
    }

    @Override
    public ReplyToHandler getReplyToHandler() {
      return null;
    }

    @Override
    public Object getReplyToDestination() {
      return null;
    }

    @Override
    public boolean isSynchronous() {
      return false;
    }

    @Override
    public void setMessage(MuleMessage message) {}

    @Override
    public DataType getFlowVariableDataType(String key) {
      return null;
    }

    @Override
    public void clearFlowVariables() {}

    @Override
    public Object getFlowVariable(String key) {
      return null;
    }

    @Override
    public void setFlowVariable(String key, Object value) {}

    @Override
    public void setFlowVariable(String key, Object value, DataType dataType) {

    }

    @Override
    public void removeFlowVariable(String key) {}

    @Override
    public Set<String> getFlowVariableNames() {
      return Collections.emptySet();
    }

    @Override
    public boolean isNotificationsEnabled() {
      return true;
    }

    @Override
    public void setEnableNotifications(boolean enabled) {}

    @Override
    public boolean isAllowNonBlocking() {
      return false;
    }

    @Override
    public FlowCallStack getFlowCallStack() {
      return null;
    }

    @Override
    public ProcessorsTrace getProcessorsTrace() {
      return null;
    }

    @Override
    public SecurityContext getSecurityContext() {
      return null;
    }

    @Override
    public void setSecurityContext(SecurityContext context) {

    }

    @Override
    public void setError(Error error) {

    }

    @Override
    public Correlation getCorrelation() {
      return null;
    }

    @Override
    public String getCorrelationId() {
      return null;
    }

    @Override
    public MuleEvent getParent() {
      return null;
    }
  }

}
