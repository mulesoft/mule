/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.management.mbean;

import java.util.List;

import javax.management.MBeanServer;

public class MBeanServerFactory 
{
    public static MBeanServer getOrCreateMBeanServer()
    {
        MBeanServer server;
        
        List servers = javax.management.MBeanServerFactory.findMBeanServer(null);
        if (servers != null && servers.size() > 0)
        {
            server = (MBeanServer) servers.get(0);
        }
        else
        {
            server = javax.management.MBeanServerFactory.createMBeanServer();
        }
        return server;
    }
}


