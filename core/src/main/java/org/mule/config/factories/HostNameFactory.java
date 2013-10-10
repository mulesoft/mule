/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.factories;

import org.mule.api.config.PropertyFactory;

import java.net.InetAddress;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Extracts the local hostname from the local system
 */
public class HostNameFactory implements PropertyFactory
{
    protected static final Log logger = LogFactory.getLog(HostNameFactory.class);

    public Object create(Map<?, ?> props) throws Exception
    {
        // we could use getCanonicalHostName here.  however, on machines behind
        // NAT firewalls it seems that is often the NAT address, which corresponds
        // to an interface on the firewall, not on the local machine.
        try
        {
            return InetAddress.getLocalHost().getHostName();
        }
        catch (Exception e)
        {
            logger.warn("Unable to resolve hostname, defaulting to 'localhost': " + e.getMessage(), e);
            return "localhost";
        }
    }

}
