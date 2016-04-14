/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.factories;

import org.mule.api.config.PropertyFactory;
import org.mule.util.NetworkUtils;

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
            return NetworkUtils.getLocalHost().getHostName();
        }
        catch (Exception e)
        {
            logger.warn("Unable to resolve hostname, defaulting to 'localhost': " + e.getMessage(), e);
            return "localhost";
        }
    }

}
