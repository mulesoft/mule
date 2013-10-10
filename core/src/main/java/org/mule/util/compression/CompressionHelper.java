/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.compression;

import org.mule.util.ClassUtils;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>CompressionHelper</code> a static class that provides facilities for
 * compressing and uncompressing byte arrays
 */

public final class CompressionHelper
{
    /**
     * logger used by this class
     */
    private static Log logger = LogFactory.getLog(CompressionHelper.class);

    private static CompressionStrategy defaultStrategy;

    /** Do not instanciate. */
    private CompressionHelper ()
    {
        // no-op
    }

    public static synchronized CompressionStrategy getDefaultCompressionStrategy()
    {
        if (defaultStrategy == null)
        {
            defaultStrategy = AccessController.doPrivileged(new PrivilegedAction<CompressionStrategy>()
            {
                @Override
                public CompressionStrategy run()
                {
                    try
                    {
                        Object o = ClassUtils.loadClass(CompressionStrategy.COMPRESSION_DEFAULT,
                            CompressionHelper.class).newInstance();
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Found CompressionStrategy: " + o.getClass().getName());
                        }
                        return (CompressionStrategy) o;
                    }
                    catch (Exception e)
                    {
                        // TODO MULE-863: What should we really do?  Document this?
                        logger.warn("Failed to build compression strategy: " + e.getMessage());
                    }
                    return null;
                }
            });
        }
        return defaultStrategy;
    }

}
