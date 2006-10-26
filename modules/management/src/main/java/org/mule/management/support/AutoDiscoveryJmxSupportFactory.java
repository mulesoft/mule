/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the BSD style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.util.ClassUtils;

import javax.management.ObjectName;
import java.lang.reflect.Method;

/**
 * Will discover if newer JMX version is available, otherwise fallback to JMX 1.1
 * style support.
 */
public class AutoDiscoveryJmxSupportFactory implements JmxSupportFactory
{
    /**
     * logger used by this class
     */
    private transient Log logger = LogFactory.getLog(getClass());

    /**
     * Will try to detect if JMX 1.2 or later is available, otherwise will fallback
     * to the JMX 1.1 version of the support class.
     *
     * @return matching support class instance
     * @see JmxLegacySupport
     */
    public JmxSupport newJmxSupport()
    {
        // TODO cache the support class instance
        Class clazz = ObjectName.class;
        // method escape() is available since JMX 1.2
        Method method = ClassUtils.getMethod("quote", new Class[]{String.class}, clazz);

        final boolean jmxModernAvailable = method == null;
        JmxSupport jmxSupport = jmxModernAvailable ? new JmxLegacySupport() : new JmxModernSupport();
        if (logger.isDebugEnabled())
        {
            logger.debug("JMX support class is " + jmxSupport.getClass().getName());
        }
        return jmxSupport;
    }

}
