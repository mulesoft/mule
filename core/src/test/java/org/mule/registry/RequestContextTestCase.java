/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.registry;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.OptimizedRequestContext;
import org.mule.RequestContext;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.ThreadSafeAccess;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.notification.FlowCallStack;
import org.mule.api.context.notification.ProcessorsTrace;
import org.mule.api.security.Credentials;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.ReplyToHandler;
import org.mule.management.stats.ProcessingTime;
import org.mule.message.DefaultExceptionPayload;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.OutputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

public class RequestContextTestCase extends AbstractMuleTestCase
{

    private boolean threadSafeEvent;
    private MuleContext muleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);

    @Test
    public void testSetExceptionPayloadAcrossThreads() throws InterruptedException
    {
        threadSafeEvent = true;
        MuleEvent event = new DummyEvent();
        runThread(event, false);
        runThread(event, true);
    }

    @Test
    public void testFailureWithoutThreadSafeEvent() throws InterruptedException
    {
        threadSafeEvent = false;
        MuleEvent event = new DummyEvent();
        runThread(event, false);
        runThread(event, true);
    }

    protected void runThread(MuleEvent event, boolean doTest) throws InterruptedException
    {
        AtomicBoolean success = new AtomicBoolean(false);
        Thread thread = new Thread(new SetExceptionPayload(event, success));
        thread.start();
        thread.join();
        if (doTest)
        {
            assertEquals(threadSafeEvent, success.get());
        }
    }

    private class SetExceptionPayload implements Runnable
    {

        private MuleEvent event;
        private AtomicBoolean success;

        public SetExceptionPayload(MuleEvent event, AtomicBoolean success)
        {
            this.event = event;
            this.success = success;
        }

        @Override
        public void run()
        {
            try
            {
                OptimizedRequestContext.unsafeSetEvent(event);
                RequestContext.setExceptionPayload(new DefaultExceptionPayload(new Exception()));
                success.set(true);
            }
            catch (RuntimeException e)
            {
                logger.error("error in thread", e);
            }
        }

    }

    private class DummyEvent implements MuleEvent, ThreadSafeAccess
    {

        private MuleMessage message = new DefaultMuleMessage(null, muleContext);

        @Override
        public MuleMessage getMessage()
        {
            return message;
        }

        @Override
        public Credentials getCredentials()
        {
            return null;
        }

        @Override
        public byte[] getMessageAsBytes() throws MuleException
        {
            return new byte[0];
        }

        @Override
        public Object transformMessage() throws TransformerException
        {
            return null;
        }

        @Override
        public Object transformMessage(Class outputType) throws TransformerException
        {
            return null;
        }

        @Override
        @Deprecated
        public byte[] transformMessageToBytes() throws TransformerException
        {
            return new byte[0];
        }

        @Override
        public String transformMessageToString() throws TransformerException
        {
            return null;
        }

        @Override
        public String getMessageAsString() throws MuleException
        {
            return null;
        }

        @Override
        public <T> T transformMessage(DataType<T> outputType) throws TransformerException
        {
            return null;
        }

        @Override
        public String getMessageAsString(String encoding) throws MuleException
        {
            return null;
        }

        @Override
        public String getId()
        {
            return null;
        }

        @Override
        public Object getProperty(String key)
        {
            return null;
        }

        @Override
        public Object getProperty(String key, Object defaultValue)
        {
            return null;
        }

        @Override
        public MuleSession getSession()
        {
            return null;
        }

        @Override
        public FlowConstruct getFlowConstruct()
        {
            return null;
        }

        @Override
        public boolean isStopFurtherProcessing()
        {
            return false;
        }

        @Override
        public void setStopFurtherProcessing(boolean stopFurtherProcessing)
        {
            // no action
        }

        @Override
        public int getTimeout()
        {
            return 0;
        }

        @Override
        public void setTimeout(int timeout)
        {
            // no action
        }

        @Override
        public OutputStream getOutputStream()
        {
            return null;
        }

        @Override
        public String getEncoding()
        {
            return null;
        }

        @Override
        public MuleContext getMuleContext()
        {
            return null;
        }

        @Override
        public void assertAccess(boolean write)
        {
            // no action
        }

        @Override
        public void resetAccessControl()
        {
            // no action
        }

        @Override
        public ThreadSafeAccess newThreadCopy()
        {
            if (threadSafeEvent)
            {
                return new DummyEvent();
            }
            else
            {
                return this;
            }
        }

        @Override
        public ProcessingTime getProcessingTime()
        {
            return null;
        }

        @Override
        public MessageExchangePattern getExchangePattern()
        {
            return null;
        }

        @Override
        public boolean isTransacted()
        {
            return false;
        }

        @Override
        public URI getMessageSourceURI()
        {
            return URI.create("test://test");
        }

        @Override
        public String getMessageSourceName()
        {
            return "test";
        }

        @Override
        public ReplyToHandler getReplyToHandler()
        {
            return null;
        }

        @Override
        public Object getReplyToDestination()
        {
            return null;
        }

        @Override
        public void captureReplyToDestination()
        {
        }

        @Override
        public boolean isSynchronous()
        {
            return false;
        }

        @Override
        public void setMessage(MuleMessage message)
        {
        }

        @Override
        public DataType<?> getFlowVariableDataType(String key)
        {
            return null;
        }

        @Override
        public void clearFlowVariables()
        {
        }

        @Override
        public DataType<?> getSessionVariableDataType(String key)
        {
            return null;
        }

        @Override
        public Object getFlowVariable(String key)
        {
            return null;
        }

        @Override
        public void setFlowVariable(String key, Object value)
        {
        }

        @Override
        public void setFlowVariable(String key, Object value, DataType dataType)
        {

        }

        @Override
        public void removeFlowVariable(String key)
        {
        }

        @Override
        public Set<String> getFlowVariableNames()
        {
            return Collections.emptySet();
        }

        @Override
        public Object getSessionVariable(String key)
        {
            return null;
        }

        @Override
        public void setSessionVariable(String key, Object value)
        {
        }

        @Override
        public void setSessionVariable(String key, Serializable value, DataType dataType)
        {

        }

        @Override
        public void removeSessionVariable(String key)
        {
        }

        @Override
        public Set<String> getSessionVariableNames()
        {
            return Collections.emptySet();
        }

        @Override
        public void clearSessionVariables()
        {
        }

        @Override
        public boolean isNotificationsEnabled()
        {
            return true;
        }

        @Override
        public void setEnableNotifications(boolean enabled)
        {
        }

        @Override
        public boolean isAllowNonBlocking()
        {
            return false;
        }

        @Override
        public FlowCallStack getFlowCallStack()
        {
            return null;
        }

        @Override
        public ProcessorsTrace getProcessorsTrace()
        {
            return null;
        }
    }

}
