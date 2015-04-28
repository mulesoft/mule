/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.mule.api.config.ThreadingProfile;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.concurrent.Callable;

import junit.framework.Assert;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.mockito.Mockito;

@SmallTest
public class DirectThreadingProfileTestCase extends AbstractMuleTestCase
{

    private Thread workThread;

    @Test
    public void testDirectThreadingProfile()
    {
        ThreadingProfile threadingProfile = new DirectThreadingProfile(null);

        assertThat(threadingProfile.getMaxThreadsActive(), equalTo(0));
        assertThat(threadingProfile.getMaxThreadsIdle(), equalTo(0));
        assertThat(threadingProfile.getThreadTTL(), equalTo(0l));
        assertThat(threadingProfile.getMaxBufferSize(), equalTo(0));
        assertThat(threadingProfile.getPoolExhaustedAction(), equalTo(0));
        assertThat(threadingProfile.getThreadWaitTimeout(), equalTo(0l));
        assertFalse(threadingProfile.isDoThreading());

        threadingProfile.createPool().submit(new Callable<Object>()
        {
            @Override
            public Object call() throws Exception
            {
                workThread = Thread.currentThread();
                return null;
            }
        });

        assertThat(workThread, equalTo(Thread.currentThread()));
    }

}
