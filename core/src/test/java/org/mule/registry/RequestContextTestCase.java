/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.registry;

import org.mule.DefaultMuleMessage;
import org.mule.OptimizedRequestContext;
import org.mule.RequestContext;
import org.mule.api.MuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.ThreadSafeAccess;
import org.mule.api.component.Component;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.security.Credentials;
import org.mule.api.transformer.TransformerException;
import org.mule.message.DefaultExceptionPayload;
import org.mule.tck.AbstractMuleTestCase;

import java.io.OutputStream;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

public class RequestContextTestCase extends AbstractMuleTestCase
{

    private boolean threadSafeEvent;

    public void testSetExceptionPayloadAcrossThreads() throws InterruptedException
    {
        threadSafeEvent = true;
        MuleEvent event = new DummyEvent();
        runThread(event, false);
        runThread(event, true);
    }

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

        private MuleMessage message = new DefaultMuleMessage(null);

        public MuleMessage getMessage()
        {
            return message;
        }

        public Credentials getCredentials()
        {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public byte[] getMessageAsBytes() throws MuleException
        {
            return new byte[0];  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Object transformMessage() throws TransformerException
        {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Object transformMessage(Class outputType) throws TransformerException
        {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public byte[] transformMessageToBytes() throws TransformerException
        {
            return new byte[0];  //To change body of implemented methods use File | Settings | File Templates.
        }

        public String transformMessageToString() throws TransformerException
        {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public String getMessageAsString() throws MuleException
        {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public String getTransformedMessageAsString(String encoding) throws TransformerException
        {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public String getMessageAsString(String encoding) throws MuleException
        {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public String getId()
        {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Object getProperty(String name, boolean exhaustiveSearch)
        {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Object getProperty(String name, Object defaultValue, boolean exhaustiveSearch)
        {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public ImmutableEndpoint getEndpoint()
        {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public MuleSession getSession()
        {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Component getComponent()
        {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public boolean isStopFurtherProcessing()
        {
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void setStopFurtherProcessing(boolean stopFurtherProcessing)
        {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public boolean isSynchronous()
        {
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void setSynchronous(boolean value)
        {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public int getTimeout()
        {
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void setTimeout(int timeout)
        {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public OutputStream getOutputStream()
        {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public String getEncoding()
        {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public MuleContext getMuleContext()
        {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void assertAccess(boolean write)
        {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void resetAccessControl()
        {
            //To change body of implemented methods use File | Settings | File Templates.
        }

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
    }

}
