/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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


