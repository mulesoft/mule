/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.factories;

import org.mule.config.PropertyFactory;

import java.net.InetAddress;
import java.util.Map;

/**
 * Extracts the local hostname from the local system
 */
public class HostNameFactory implements PropertyFactory
{

    public Object create(Map props) throws Exception
    {
        // we could use getCanonicalHostName here.  however, on machines behind
        // NAT firewalls it seems that is often the NAT address, which corresponds
        // to an interface on the firewall, not on the local machine.
        return InetAddress.getLocalHost().getHostName();
    }

}
