/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import static java.util.Arrays.asList;
import static java.util.Arrays.sort;
import static org.mule.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.module.launcher.MuleFoldersUtil.getUserLibFolder;
import org.mule.api.MuleContext;
import org.mule.api.agent.Agent;
import org.mule.config.MuleManifest;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.NetworkUtils;
import org.mule.util.SecurityUtils;
import org.mule.util.SplashScreen;
import org.mule.util.StringUtils;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class MuleContainerStartupSplashScreen extends SplashScreen
{
    public void doBody()
    {
        String notset = CoreMessages.notSet().getMessage();

        // Mule Version, Timestamp, and Server ID
        Manifest mf = MuleManifest.getManifest();
        Attributes att = mf.getMainAttributes();
        if (att.values().size() > 0)
        {
            doBody(StringUtils.defaultString(MuleManifest.getProductDescription(), notset));
            doBody(String.format("%s Build: %s",
                                 CoreMessages.version().getMessage(),
                                 StringUtils.defaultString(MuleManifest.getBuildNumber(), notset)));

            doBody(StringUtils.defaultString(MuleManifest.getVendorName(), notset));
            doBody(StringUtils.defaultString(MuleManifest.getProductMoreInfo(), notset));
        }
        else
        {
            doBody(CoreMessages.versionNotSet().getMessage());
        }
        doBody(" ");

        // TODO maybe be more precise and count from container bootstrap time?
        doBody(CoreMessages.serverStartedAt(System.currentTimeMillis()).getMessage());

        doBody(String.format("JDK: %s (%s)",
                             System.getProperty("java.version"),
                             System.getProperty("java.vm.info")));

        String patch = System.getProperty("sun.os.patch.level", null);

        doBody(String.format("OS: %s%s (%s, %s)",
                             System.getProperty("os.name"),
                             (patch != null && !"unknown".equalsIgnoreCase(patch) ? " - " + patch : ""),
                             System.getProperty("os.version"),
                             System.getProperty("os.arch")));
        try
        {
            InetAddress host = NetworkUtils.getLocalHost();
            doBody(String.format("Host: %s (%s)", host.getHostName(), host.getHostAddress()));
        }
        catch (UnknownHostException e)
        {
            // ignore
        }
        if (!SecurityUtils.isDefaultSecurityModel())
        {
            doBody("Security model: " + SecurityUtils.getSecurityModel());
        }
        if (RUNTIME_VERBOSE_PROPERTY.isEnabled())
        {
            listPatchesIfPresent();
            listMuleSystemProperties();
        }
    }

    private void listPatchesIfPresent()
    {
        File patchesDirectory = getUserLibFolder();
        if (patchesDirectory != null && patchesDirectory.exists())
        {
            String[] patches = patchesDirectory.list();
            sort(patches);
            listItems(asList(patches), "Applied patches and libraries:");
        }
    }

    private void listMuleSystemProperties()
    {
        Map<String, String> muleProperties = new HashMap<>();
        for (String property : System.getProperties().stringPropertyNames())
        {
            if (property.startsWith(SYSTEM_PROPERTY_PREFIX))
            {
                muleProperties.put(property, System.getProperty(property));
            }
        }
        listItems(muleProperties, "Mule system properties:");
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
