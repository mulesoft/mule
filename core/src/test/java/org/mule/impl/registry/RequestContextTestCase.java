/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.registry;

import org.mule.api.MuleContext;
import org.mule.impl.MuleMessage;
import org.mule.impl.RequestContext;
import org.mule.impl.ThreadSafeAccess;
import org.mule.impl.OptimizedRequestContext;
import org.mule.impl.message.ExceptionPayload;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOException;
import org.mule.umo.UMOSession;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.transformer.TransformerException;
import org.mule.umo.security.UMOCredentials;

import java.io.OutputStream;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

public class RequestContextTestCase extends AbstractMuleTestCase
{

    private boolean threadSafeEvent;

    public void testSetExceptionPayloadAcrossThreads() throws InterruptedException
    {
        threadSafeEvent = true;
        UMOEvent event = new DummyEvent();
        runThread(event, false);
        runThread(event, true);
    }

    public void testFailureWithoutThreadSafeEvent() throws InterruptedException
    {
        threadSafeEvent = false;
        UMOEvent event = new DummyEvent();
        runThread(event, false);
        runThread(event, true);
    }

    protected void runThread(UMOEvent event, boolean doTest) throws InterruptedException
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

        private UMOEvent event;
        private AtomicBoolean success;

        public SetExceptionPayload(UMOEvent event, AtomicBoolean success)
        {
            this.event = event;
            this.success = success;
        }

        public void run()
        {
            try
            {
                OptimizedRequestContext.unsafeSetEvent(event);
                RequestContext.setExceptionPayload(new ExceptionPayload(new Exception()));
                success.set(true);
            }
            catch (RuntimeException e)
            {
                logger.error("error in thread", e);
            }
        }

    }

    private class DummyEvent implements UMOEvent, ThreadSafeAccess
    {

        private UMOMessage message = new MuleMessage(null);

        public UMOMessage getMessage()
        {
            return message;
        }

        public UMOCredentials getCredentials()
        {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public byte[] getMessageAsBytes() throws UMOException
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

        public String getMessageAsString() throws UMOException
        {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public String getTransformedMessageAsString(String encoding) throws TransformerException
        {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public String getMessageAsString(String encoding) throws UMOException
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

        public UMOImmutableEndpoint getEndpoint()
        {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public UMOSession getSession()
        {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public UMOComponent getComponent()
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
