/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.management.support;

import org.mule.api.MuleContext;
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
    public String getDomainName(MuleContext context)
    {
        return getDomainName(context, true);
    }

    public String getDomainName(MuleContext context, boolean resolveClash)
    {
        String domain = DEFAULT_JMX_DOMAIN_PREFIX;
        String instanceId = StringUtils.defaultString(context.getConfiguration().getId());
        if (instanceId.length() > 0)
        {
            domain += "." + instanceId;
        }

        JmxRegistrationContext ctx = JmxRegistrationContext.getCurrent(context);

        String resolvedDomain = ctx.getResolvedDomain();
        if (resolveClash)
        {
            if (StringUtils.isBlank(resolvedDomain))
            {
                domain = resolveDomainClash(domain);
                ctx.setResolvedDomain(domain);
            }
        }

        return domain;
    }
}
