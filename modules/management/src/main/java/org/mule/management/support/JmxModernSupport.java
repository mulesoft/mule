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

import org.mule.MuleManager;
import org.mule.util.StringUtils;

import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.MBeanServer;
import java.util.List;
import java.util.Arrays;

/**
 * Support class using JMX 1.2 and newer calls.
 */
public class JmxModernSupport extends AbstractJmxSupport
{

    /** {@inheritDoc} */
    public String escape(String input)
    {
        return ObjectName.quote(input);
    }

    /**
     * For modern JMX implementation just delegate to a standard factory method.
     * @param name object name
     * @return ObjectName for MBeanServer
     * @throws MalformedObjectNameException for invalid names
     */
    public ObjectName getObjectName(String name) throws MalformedObjectNameException
    {
        return ObjectName.getInstance(name);
    }
}
