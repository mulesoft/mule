/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl;

import org.mule.config.MuleProperties;
import org.mule.impl.model.direct.DirectComponent;
import org.mule.providers.DefaultMessageAdapter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

import java.util.Map;

public class ThreadSafeAccessTestCase extends AbstractMuleTestCase
{

    protected void doSetUp() throws Exception
    {
        System.setProperty(MuleProperties.MULE_THREAD_UNSAFE_MESSAGES_PROPERTY, "false");
    }

    public void testMessage() throws InterruptedException
    {
        basicPattern(new MuleMessage(new Object(), (Map)null));
        newCopy(new MuleMessage(new Object(), (Map)null));
        resetAccessControl(new MuleMessage(new Object(), (Map)null));
    }

    public void testAdapter() throws InterruptedException
    {
        basicPattern(new DefaultMessageAdapter(new Object()));
        newCopy(new DefaultMessageAdapter(new Object()));
        resetAccessControl(new DefaultMessageAdapter(new Object()));
    }

    public void testEvent() throws Exception
    {
        basicPattern(dummyEvent());
        newCopy(dummyEvent());
        resetAccessControl(dummyEvent());
    }

    public void testDisable() throws InterruptedException
    {
        try
        {
            System.setProperty(MuleProperties.MULE_THREAD_UNSAFE_MESSAGES_PROPERTY, "true");
            ThreadSafeAccess target = new DefaultMessageAdapter(new Object());
            newThread(target, false, new boolean[]{true, true, false, true});
            newThread(target, false, new boolean[]{false});
            newThread(target, false, new boolean[]{true});
        }
        finally
        {
            System.getProperties().remove(MuleProperties.MULE_THREAD_UNSAFE_MESSAGES_PROPERTY);
        }
    }

    protected ThreadSafeAccess dummyEvent() throws Exception
    {
        UMOMessage message = new MuleMessage(new Object(), (Map) null);
        return new MuleEvent(message, MuleTestUtils.getTestEndpoint("test",
            UMOImmutableEndpoint.ENDPOINT_TYPE_RECEIVER, managementContext), new MuleSession(new DirectComponent(
            new MuleDescriptor(""), null)), false);
    }

    protected void resetAccessControl(ThreadSafeAccess target) throws InterruptedException
    {
        target.assertAccess(true);
        newThread(target, true, new boolean[]{true});
        target.resetAccessControl();
        newThread(target, false, new boolean[]{true});
    }

    protected void basicPattern(ThreadSafeAccess target) throws InterruptedException
    {
        newThread(target, false, new boolean[]{true, true, false, true});
        newThread(target, false, new boolean[]{false});
        newThread(target, true, new boolean[]{true});
    }

    protected void newCopy(ThreadSafeAccess target) throws InterruptedException
    {
        basicPattern(target);
        basicPattern(target.newThreadCopy());
    }

    protected void newThread(ThreadSafeAccess target, boolean error, boolean[] pattern) throws InterruptedException
    {
        Caller caller = new Caller(target, pattern);
        Thread thread =  new Thread(caller);
        thread.start();
        thread.join();
        assertEquals(error, caller.isError());
    }

    protected static class Caller implements Runnable
    {

        private boolean isError = false;
        private ThreadSafeAccess target;
        private boolean[] write;

        public Caller(ThreadSafeAccess target, boolean[] write)
        {
            this.target = target;
            this.write = write;
        }

        public void run()
        {
            try
            {
                for (int i = 0; i < write.length; i++)
                {
                    target.assertAccess(write[i]);
                }
            }
            catch (IllegalStateException e)
            {
                isError = true;
            }
        }

        public boolean isError()
        {
            return isError;
        }

    }

}
