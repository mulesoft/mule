/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.config;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class ThreadingProfileTestCase extends AbstractMuleTestCase
{

    @Test
    public void usesDefaultValues() throws Exception
    {
        ThreadingProfile defaultThreadingProfile = ThreadingProfile.DEFAULT_THREADING_PROFILE;

        assertThat(defaultThreadingProfile.getMaxThreadsActive(), equalTo(16));
        assertThat(defaultThreadingProfile.getMaxThreadsIdle(), equalTo(1));
        assertThat(defaultThreadingProfile.getMaxBufferSize(), equalTo(0));
        assertThat(defaultThreadingProfile.getThreadTTL(), equalTo(60000L));
        assertThat(defaultThreadingProfile.getThreadWaitTimeout(), equalTo(30000L));
    }
}