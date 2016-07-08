/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.functional.junit4.FunctionalTestCase;

import org.junit.Test;

public class DefaultThreadingProfileTestCase extends FunctionalTestCase
{

    @Override
    protected String[] getConfigFiles()
    {
        return new String[0];
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
