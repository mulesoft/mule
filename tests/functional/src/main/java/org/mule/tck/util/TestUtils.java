/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.util;


import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.context.MuleContextBuilder;
import org.mule.api.context.MuleContextFactory;
import org.mule.api.context.notification.MuleContextNotificationListener;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.context.notification.MuleContextNotification;
import org.mule.util.concurrent.Latch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class TestUtils
{

    public static MuleContext loadConfiguration(String configuration) throws MuleException, InterruptedException
    {
        MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
        List<ConfigurationBuilder> builders = new ArrayList<ConfigurationBuilder>();
        builders.add(new SpringXmlConfigurationBuilder(configuration));
        MuleContextBuilder contextBuilder = new DefaultMuleContextBuilder();
        MuleContext muleContext = muleContextFactory.createMuleContext(builders, contextBuilder);
        final AtomicReference<Latch> contextStartedLatch = new AtomicReference<Latch>();
        contextStartedLatch.set(new Latch());
        muleContext.registerListener(new MuleContextNotificationListener<MuleContextNotification>()
        {
            @Override
            public void onNotification(MuleContextNotification notification)
            {
                if (notification.getAction() == MuleContextNotification.CONTEXT_STARTED)
                {
                    contextStartedLatch.get().countDown();
                }
            }
        });
        muleContext.start();
        contextStartedLatch.get().await(20, TimeUnit.SECONDS);
        
        return muleContext;
    }

}
