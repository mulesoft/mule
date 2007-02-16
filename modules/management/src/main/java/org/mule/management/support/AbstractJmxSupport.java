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

import org.mule.MuleManager;
import org.mule.util.StringUtils;

import java.util.Collection;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

public abstract class AbstractJmxSupport implements JmxSupport
{

    /**
     * Resolve JMX domain clash by adding an incremented number suffix to the name. E.g. if
     * 'Mule.TradeProcessor' is already registered with the accessible MBeanServer, will return
     * 'Mule.TradeProcessor.1'. If the latter one is already registered, will return
     * 'Mule.TradeProcessor.2' and so on.
     * <p/>
     * If no clash detected, returns the domain name unmodified.
     * @param domain domain name causing a conflict
     * @return resolved non-conflicting domain name
     */
    protected String resolveDomainClash(String domain)
    {
        List servers = MBeanServerFactory.findMBeanServer(null);
        if (servers.isEmpty())
        {
            throw new IllegalStateException("MBeanServer is not available.");
        }
        MBeanServer server = (MBeanServer) servers.get(0);

        Collection registeredDomains = getDomains(server);
        int counter = 1;
        // Just append .<n> suffix to the domain for a start
        if (registeredDomains.contains(domain))
        {
            domain += "." + counter;
        }
        // recheck in case there were any suffixed domains already
        while (registeredDomains.contains(domain))
        {
            // append .<n> until we succeed
            domain = domain.substring(0, domain.lastIndexOf(".") + 1) + ++counter;
        }

        return domain;
    }

    /**
     * List all domains of this MBean server.
     * @param server server to contact
     * @return a collection of unique JMX domains
     */
    protected abstract Collection getDomains(MBeanServer server);

    /** {@inheritDoc} */
    public String getDomainName()
    {
        // TODO add some config options to the JmxAgent
        String domain = DEFAULT_JMX_DOMAIN_PREFIX;
        String instanceId = StringUtils.defaultString(MuleManager.getInstance().getId());
        if (instanceId.length() > 0)
        {
            domain += "." + instanceId;
        }

        JmxRegistrationContext ctx = JmxRegistrationContext.getCurrent();

        String resolvedDomain = ctx.getResolvedDomain();
        if (StringUtils.isBlank(resolvedDomain))
        {
            domain = resolveDomainClash(domain);
            ctx.setResolvedDomain(domain);
        }

        return domain;
    }
}
