/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.spring;

import org.mule.api.config.ThreadingProfile;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DefaultThreadingProfileTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "";
    }

    @Test
    public void testDefaultThreadingProfile(){
        assertNotNull(muleContext.getDefaultThreadingProfile());
        assertEquals(ThreadingProfile.DEFAULT_MAX_THREADS_ACTIVE, muleContext.getDefaultThreadingProfile().getMaxThreadsActive());
        assertEquals(ThreadingProfile.DEFAULT_MAX_THREADS_IDLE, muleContext.getDefaultThreadingProfile().getMaxThreadsIdle());
        assertEquals(ThreadingProfile.DEFAULT_MAX_THREAD_TTL, muleContext.getDefaultThreadingProfile().getThreadTTL());
        assertEquals(ThreadingProfile.DEFAULT_MAX_BUFFER_SIZE, muleContext.getDefaultThreadingProfile().getMaxBufferSize());
        assertEquals(ThreadingProfile.DEFAULT_POOL_EXHAUST_ACTION, muleContext.getDefaultThreadingProfile().getPoolExhaustedAction());
        assertEquals(ThreadingProfile.DEFAULT_THREAD_WAIT_TIMEOUT, muleContext.getDefaultThreadingProfile().getThreadWaitTimeout());
        assertEquals(ThreadingProfile.DEFAULT_DO_THREADING, muleContext.getDefaultThreadingProfile().isDoThreading());
    }

}
