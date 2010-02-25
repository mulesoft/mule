/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.pool;

import org.mule.api.MuleContext;
import org.mule.api.MuleRuntimeException;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.context.MuleContextAware;
import org.mule.config.i18n.MessageFactory;
import org.mule.util.ClassUtils;

import java.util.Iterator;

import javax.imageio.spi.ServiceRegistry;

import edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Uses a standard JDK's
 * <a href="http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html#Service%20Provider">SPI discovery</a>
 * mechanism to locate implementations.
 */
public abstract class ThreadPoolFactory implements MuleContextAware
{
    // keep it private, subclasses will have their own loggers, thus avoiding contention on this static one
    private static final Log logger = LogFactory.getLog(ThreadPoolFactory.class);

    private static final String PREFERRED_CONFIG_CLASSNAME = "com.mulesoft.mule.config.Preferred";
    private static boolean extensionsAvailable = ClassUtils.isClassOnPath(PREFERRED_CONFIG_CLASSNAME, ThreadPoolFactory.class);

    protected MuleContext muleContext;

    /**
     * @return a discovered
     */
    public static ThreadPoolFactory newInstance()
    {
        Class preferredMarker = null;
        if (extensionsAvailable)
        {
            try
            {
                preferredMarker = ClassUtils.loadClass(PREFERRED_CONFIG_CLASSNAME, ThreadPoolFactory.class);
            }
            catch (ClassNotFoundException e)
            {
                extensionsAvailable = false;
                if (logger.isDebugEnabled())
                {
                    logger.debug("Failed to load EE extensions", e);
                }
            }

        }

        /*
           There's a public (at last!) SPI mechanism in Java 6, java.util.ServiceLoader, but
           it's hidden in earlier versions.

           The javax.imageio.spi.ServiceRegistry, while not belonging here at first look, is
           perfectly functional. Underneath, it wraps the sun.misc.Service class, which does the
           lookup. The latter has been available since Java 1.3 and standardized internally at Sun.
           Also, Sun, Bea JRockit and IBM JDK all have it readily available for use, so it's safe to
           rely on.
        */
        final Iterator<ThreadPoolFactory> it = ServiceRegistry.lookupProviders(ThreadPoolFactory.class);
        ThreadPoolFactory candidate = null;
        while (it.hasNext())
        {
            ThreadPoolFactory threadPoolFactory = it.next();
            // if found a preferred one, grab it and stop
            if (extensionsAvailable && preferredMarker.isAssignableFrom(threadPoolFactory.getClass()))
            {
                return threadPoolFactory;
            }
            else
            {
                // only keep around the first non-preferred one we found,
                // in case non-preferred was found before preferred
                if (candidate == null)
                {
                    candidate = threadPoolFactory;
                }
            }
        }

        if (candidate != null)
        {
            return candidate;
        }

        throw new MuleRuntimeException(MessageFactory.createStaticMessage(
                "Couldn't find config via SPI mechanism. Corrupted Mule core jar?"
        ));
    }

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public abstract ThreadPoolExecutor createPool(String name, ThreadingProfile tp);
}
