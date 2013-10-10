/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.management.support;

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
