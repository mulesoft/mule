/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher;

import static java.util.Arrays.asList;
import static java.util.Arrays.sort;
import static org.apache.commons.lang.StringUtils.defaultString;
import static org.mule.runtime.container.api.MuleFoldersUtil.getServicesFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getUserLibFolder;
import static org.mule.runtime.core.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.agent.Agent;
import org.mule.runtime.core.config.MuleManifest;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.util.NetworkUtils;
import org.mule.runtime.core.util.SecurityUtils;
import org.mule.runtime.core.util.SplashScreen;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class MuleContainerStartupSplashScreen extends SplashScreen {

  public void doBody() {
    String notset = CoreMessages.notSet().getMessage();

    // Mule Version, Timestamp, and Server ID
    Manifest mf = MuleManifest.getManifest();
    Attributes att = mf.getMainAttributes();
    if (att.values().size() > 0) {
      doBody(defaultString(MuleManifest.getProductDescription(), notset));
      doBody(String.format("%s Build: %s", CoreMessages.version().getMessage(),
                           defaultString(MuleManifest.getBuildNumber(), notset)));

      doBody(defaultString(MuleManifest.getVendorName(), notset));
      doBody(defaultString(MuleManifest.getProductMoreInfo(), notset));
    } else {
      doBody(CoreMessages.versionNotSet().getMessage());
    }
    doBody(" ");

    // TODO maybe be more precise and count from container bootstrap time?
    doBody(CoreMessages.serverStartedAt(System.currentTimeMillis()).getMessage());

    doBody(String.format("JDK: %s (%s)", System.getProperty("java.version"), System.getProperty("java.vm.info")));

    String patch = System.getProperty("sun.os.patch.level", null);

    doBody(String.format("OS: %s%s (%s, %s)", System.getProperty("os.name"),
                         (patch != null && !"unknown".equalsIgnoreCase(patch) ? " - " + patch : ""),
                         System.getProperty("os.version"), System.getProperty("os.arch")));
    try {
      InetAddress host = NetworkUtils.getLocalHost();
      doBody(String.format("Host: %s (%s)", host.getHostName(), host.getHostAddress()));
    } catch (UnknownHostException e) {
      // ignore
    }
    if (!SecurityUtils.isDefaultSecurityModel()) {
      doBody("Security model: " + SecurityUtils.getSecurityModel());
    }
    if (RUNTIME_VERBOSE_PROPERTY.isEnabled()) {
      listServicesIfPresent();
      listPatchesIfPresent();
      listMuleSystemProperties();
    }
  }

  private void listServicesIfPresent() {
    File servicesDirectory = getServicesFolder();
    if (servicesDirectory != null && servicesDirectory.exists()) {
      listItems(asList(servicesDirectory.list()), "Mule services:");
    }
  }

  private void listPatchesIfPresent() {
    File patchesDirectory = getUserLibFolder();
    if (patchesDirectory != null && patchesDirectory.exists()) {
      String[] patches = patchesDirectory.list((dir, name) -> name.startsWith("SE-"));
      sort(patches);
      listItems(asList(patches), "Applied patches:");
    }
  }

  private void listMuleSystemProperties() {
    Map<String, String> muleProperties = new HashMap<>();
    System.getProperties().stringPropertyNames().stream().filter(property -> property.startsWith(SYSTEM_PROPERTY_PREFIX))
        .forEach(property -> muleProperties.put(property, System.getProperty(property)));
    listItems(muleProperties, "Mule system properties:");
  }

  @Override
  protected void doFooter(MuleContext context) {
    // Mule Agents
    if (!body.isEmpty()) {
      footer.add(" ");
    }
    // List agents
    Collection<Agent> agents = context.getRegistry().lookupObjects(Agent.class);
    if (agents.size() == 0) {
      footer.add(CoreMessages.agentsRunning().getMessage() + " " + CoreMessages.none().getMessage());
    } else {
      footer.add(CoreMessages.agentsRunning().getMessage());
      for (Agent agent : agents) {
        footer.add("  " + agent.getDescription());
      }
    }
  }
}
