/* 
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.util.compression;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.util.ClassUtils;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * <code>CompressionHelper</code> a static class that provides facilities for
 * compressing and uncompressing byte arrays
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class CompressionHelper
{
    /**
     * logger used by this class
     */
    private static transient Log logger = LogFactory.getLog(CompressionHelper.class);

    private static transient CompressionStrategy defaultStrategy;

    synchronized public static CompressionStrategy getDefaultCompressionStrategy()
    {
        if (defaultStrategy == null) {
            defaultStrategy = (CompressionStrategy)AccessController.doPrivileged(new PrivilegedAction() {
                public Object run()
                {
                    try {
                        Object o = ClassUtils.loadClass(CompressionStrategy.COMPRESSION_DEFAULT,
                                CompressionHelper.class).newInstance();
                        if (logger.isDebugEnabled()) {
                            logger.debug("Found CompressionStrategy: " + o.getClass().getName());
                        }
                        return o;
                    }
                    catch (Exception e) {
                        logger.warn("Failed to build compression strategy: " + e.getMessage());
                    }
                    return null;
                }
            });
        }
        return defaultStrategy;
    }

}
