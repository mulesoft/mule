/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.pool;

import org.mule.api.MuleContext;
import org.mule.api.MuleRuntimeException;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.context.MuleContextAware;
import org.mule.config.PreferredObjectSelector;
import org.mule.config.i18n.MessageFactory;

import java.util.Iterator;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import javax.imageio.spi.ServiceRegistry;

/**
 * Uses a standard JDK's
 * <a href="http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html#Service%20Provider">SPI discovery</a>
 * mechanism to locate implementations.
 */
public abstract class ThreadPoolFactory implements MuleContextAware
{

    protected MuleContext muleContext;

    /**
     * @return a discovered
     */
    public static ThreadPoolFactory newInstance()
    {
        /*
           There's a public (at last!) SPI mechanism in Java 6, java.util.ServiceLoader, but
           it's hidden in earlier versions.

           The javax.imageio.spi.ServiceRegistry, while not belonging here at first look, is
           perfectly functional. Underneath, it wraps the sun.misc.Service class, which does the
           lookup. The latter has been available since Java 1.3 and standardized internally at Sun.
           Also, Sun, Bea JRockit and IBM JDK all have it readily available for use, so it's safe to
           rely on.
        */
        final Iterator<ThreadPoolFactory> servicesIterator = ServiceRegistry.lookupProviders(ThreadPoolFactory.class);

        PreferredObjectSelector<ThreadPoolFactory> selector = new PreferredObjectSelector<ThreadPoolFactory>();
        ThreadPoolFactory threadPoolFactory = selector.select(servicesIterator);

        if (threadPoolFactory == null)
        {
            throw new MuleRuntimeException(MessageFactory.createStaticMessage(
                    "Couldn't find config via SPI mechanism. Corrupted Mule core jar?"
            ));
        }

        return threadPoolFactory;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public abstract ThreadPoolExecutor createPool(String name, ThreadingProfile tp);

    /**
     * By limitations of java's {@link ScheduledThreadPoolExecutor}, {@link ThreadingProfile#getMaxThreadsActive()} will
     * be ignored and a fixed pool with {@link ThreadingProfile#getMaxThreadsIdle()} will be created.
     */
    public abstract ScheduledThreadPoolExecutor createScheduledPool(String name, ThreadingProfile tp);
}
