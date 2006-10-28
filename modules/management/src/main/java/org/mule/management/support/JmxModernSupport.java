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

import javax.management.ObjectName;
import javax.management.MalformedObjectNameException;

/**
 * Support class using JMX 1.2 and newer calls.
 */
public class JmxModernSupport implements JmxSupport
{
    /** Default Mule domain prefix for all instances. */
    public static final String DEFAULT_JMX_DOMAIN_PREFIX = "Mule";

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
        StringBuffer domain = new StringBuffer(DEFAULT_JMX_DOMAIN_PREFIX);
        if (MuleManager.isInstanciated())
        {
            String instanceId = StringUtils.defaultIfEmpty(MuleManager.getInstance().getId(), StringUtils.EMPTY);
            if (instanceId.length() > 0)
            {
                domain.append(".").append(instanceId);
            }
        }
        return resolveDomainClash(domain.toString());
    }

    /**
     * Resolve JMX domain clash by adding an incremented number suffix to the name. E.g. if
     * 'Mule.TradeProcessor' is already registered with the accessible MBeanServer, will return
     * 'Mule.TradeProcessor.1'. If the latter one is already registered, will return
     * 'Mule.TradeProcessor.2' and so on.
     * <p/>
     * If no clash detected, returns the domain name unmodified. 
     * @param clashingDomain domain name causing a conflict
     * @return resolved non-conflicting domain name
     */
    protected String resolveDomainClash(String clashingDomain)
    {
        return clashingDomain;
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
