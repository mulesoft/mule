/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
            defaultStrategy = (CompressionStrategy) AccessController.doPrivileged(new PrivilegedAction()
            {
                public Object run()
                {
                    try
                    {
                        Object o = ClassUtils.loadClass(CompressionStrategy.COMPRESSION_DEFAULT,
                            CompressionHelper.class).newInstance();
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Found CompressionStrategy: " + o.getClass().getName());
                        }
                        return o;
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
