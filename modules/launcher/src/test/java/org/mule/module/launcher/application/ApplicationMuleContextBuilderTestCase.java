/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.application;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.config.ThreadingProfile;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.tck.size.SmallTest;
import org.mule.work.MuleWorkManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ApplicationMuleContextBuilderTestCase
{

    private static final String APP_NAME = "appName";

    @Mock
    private ApplicationDescriptor descriptor;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MuleConfiguration muleConfiguration;

    private ApplicationMuleContextBuilder builder;

    @Before
    public void before()
    {
        when(descriptor.getAppName()).thenReturn(APP_NAME);
        builder = new ApplicationMuleContextBuilder(descriptor);
        builder.setMuleConfiguration(muleConfiguration);
    }

    @Test
    public void threadingProfileNotShared()
    {
        MuleWorkManager mwm1 = builder.createWorkManager();
        MuleWorkManager mwm2 = builder.createWorkManager();

        assertThat(mwm1.getThreadingProfile(), not(sameInstance(mwm2.getThreadingProfile())));
        assertThat(mwm1.getThreadingProfile(), not(sameInstance(ThreadingProfile.DEFAULT_THREADING_PROFILE)));
        assertThat(mwm2.getThreadingProfile(), not(sameInstance(ThreadingProfile.DEFAULT_THREADING_PROFILE)));
    }
}
