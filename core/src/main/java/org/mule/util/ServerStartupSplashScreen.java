/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util;

import org.mule.api.MuleContext;
import org.mule.api.agent.Agent;
import org.mule.config.MuleManifest;
import org.mule.config.i18n.CoreMessages;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class ServerStartupSplashScreen extends SplashScreen
{
    @Override
    protected void doHeader(MuleContext context)
    {
        String notset = CoreMessages.notSet().getMessage();

        // Mule Version, Timestamp, and Server ID
        Manifest mf = MuleManifest.getManifest();
        Attributes att = mf.getMainAttributes();
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

        // Dev/Production mode
        // TODO for now now used, potentially a 'production' mode can disable direcotry (non-api) hot-deployment for tight app control
        //final boolean productionMode = StartupContext.get().getStartupOptions().containsKey("production");
        //header.add("Mode: " + (productionMode ? "Production" : "Development"));

        header.add(" ");
    }

    @Override
    protected void doFooter(MuleContext context)
    {
        // Mule Agents
        if (!body.isEmpty())
        {
            footer.add(" ");
        }
        //List agents
        Collection<Agent> agents = context.getRegistry().lookupObjects(Agent.class);
        if (agents.size() == 0)
        {
            footer.add(CoreMessages.agentsRunning().getMessage() + " "
                    + CoreMessages.none().getMessage());
        }
        else
        {
            footer.add(CoreMessages.agentsRunning().getMessage());
            for (Agent agent : agents)
            {
                footer.add("  " + agent.getDescription());
            }
        }
    }
}
