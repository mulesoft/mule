/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import org.mule.RegistryContext;
import org.mule.api.MuleContext;
import org.mule.api.agent.Agent;
import org.mule.config.MuleManifest;
import org.mule.config.i18n.CoreMessages;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.Manifest;

public class ServerStartupSplashScreen extends SplashScreen
{
    protected void doHeader(MuleContext context)
    {
        String notset = CoreMessages.notSet().getMessage();

        // Mule Version, Timestamp, and Server ID
        Manifest mf = MuleManifest.getManifest();
        Map att = mf.getMainAttributes();
        if (att.values().size() > 0)
        {
            header.add(StringUtils.defaultString(MuleManifest.getProductDescription(), notset));
            header.add(CoreMessages.version().getMessage() + " Build: "
                    + StringUtils.defaultString(MuleManifest.getBuildNumber(), notset));

            header.add(StringUtils.defaultString(MuleManifest.getVendorName(), notset));
            header.add(StringUtils.defaultString(MuleManifest.getProductMoreInfo(), notset));
        }
        else
        {
            header.add(CoreMessages.versionNotSet().getMessage());
        }
        header.add(" ");
        if (context.getStartDate() > 0)
        {
            header.add(CoreMessages.serverStartedAt(context.getStartDate()).getMessage());
        }
        header.add("Server ID: " + context.getConfiguration().getId());

        // JDK, Encoding, OS, and Host
        header.add("JDK: " + System.getProperty("java.version") + " (" 
            + System.getProperty("java.vm.info") + ")");
        header.add("OS encoding: " + System.getProperty("file.encoding")
                + ", Mule encoding: " + context.getConfiguration().getDefaultEncoding());
        String patch = System.getProperty("sun.os.patch.level", null);
        header.add("OS: " + System.getProperty("os.name")
                + (patch != null && !"unknown".equalsIgnoreCase(patch) ? " - " + patch : "") + " ("
                + System.getProperty("os.version") + ", " + System.getProperty("os.arch") + ")");
        try
        {
            InetAddress host = InetAddress.getLocalHost();
            header.add("Host: " + host.getHostName() + " (" + host.getHostAddress() + ")");
        }
        catch (UnknownHostException e)
        {
            // ignore
        }
        header.add(" ");
    }
   
    protected void doFooter(MuleContext context)
    {
        // Mule Agents
        if (!body.isEmpty())
        {
            footer.add(" ");
        }
        //List agents
        Collection agents = RegistryContext.getRegistry().lookupObjects(Agent.class);
        if (agents.size() == 0)
        {
            footer.add(CoreMessages.agentsRunning().getMessage() + " "
                    + CoreMessages.none().getMessage());
        }
        else
        {
            footer.add(CoreMessages.agentsRunning().getMessage());
            Agent umoAgent;
            for (Iterator iterator = agents.iterator(); iterator.hasNext();)
            {
                umoAgent = (Agent) iterator.next();
                footer.add("  " + umoAgent.getDescription());
            }
        }
    }    
}
