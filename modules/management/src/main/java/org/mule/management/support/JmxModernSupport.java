/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management.support;

import java.util.Arrays;
import java.util.Collection;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

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

    /** {@inheritDoc} */
    protected Collection getDomains(final MBeanServer server)
    {
        return Arrays.asList(server.getDomains());
    }
}
