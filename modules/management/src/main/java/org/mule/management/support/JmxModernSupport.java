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

import org.mule.util.StringUtils;
import org.mule.MuleManager;
import org.mule.management.agents.JmxAgent;

import javax.management.ObjectName;
import javax.management.MalformedObjectNameException;

/**
 * Support class using JMX 1.2 and newer calls.
 */
public class JmxModernSupport implements JmxSupport
{
    /** {@inheritDoc} */
    public String escape(String input)
    {
        return ObjectName.quote(input);
    }

    /** {@inheritDoc} */
    public String getDomainName()
    {
        // TODO detect existing Mule domains and optionally increment a new one with a suffix
        // will need to interrogate the MBeanServer, and add some config options to the JmxAgent
        String instanceSubDomain = StringUtils.EMPTY;
        if (MuleManager.isInstanciated())
        {
            String instanceId = StringUtils.defaultIfEmpty(MuleManager.getInstance().getId(), "");
            instanceSubDomain = instanceId.length() > 0 ? "." + instanceId : StringUtils.EMPTY;
        }
        return JmxAgent.DEFAULT_JMX_DOMAIN_PREFIX + instanceSubDomain;
    }

    /**
     * For modern JMX implementation just delegate to a standard factory method.
     * @param name object name
     * @return ObjectName for MBeanServer
     * @throws MalformedObjectNameException for invalid names
     */
    public ObjectName getInstance(String name) throws MalformedObjectNameException
    {
        return ObjectName.getInstance(name);
    }
}
