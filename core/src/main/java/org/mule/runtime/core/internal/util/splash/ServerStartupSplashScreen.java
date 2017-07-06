/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.splash;

import static org.apache.commons.lang3.StringUtils.defaultString;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleManifest;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.api.util.NetworkUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class ServerStartupSplashScreen extends SplashScreen {

  @Override
  protected void doHeader(MuleContext context) {
    String notset = CoreMessages.notSet().getMessage();

    // Mule Version, Timestamp, and Server ID
    Manifest mf = MuleManifest.getManifest();
    Attributes att = mf.getMainAttributes();
    if (att.values().size() > 0) {
      header.add(defaultString(MuleManifest.getProductDescription(), notset));
      header.add(CoreMessages.version().getMessage() + " Build: "
          + defaultString(MuleManifest.getBuildNumber(), notset));

      header.add(defaultString(MuleManifest.getVendorName(), notset));
      header.add(defaultString(MuleManifest.getProductMoreInfo(), notset));
    } else {
      header.add(CoreMessages.versionNotSet().getMessage());
    }
    header.add(" ");
    if (context.getStartDate() > 0) {
      header.add(CoreMessages.serverStartedAt(context.getStartDate()).getMessage());
    }
    header.add("Server ID: " + context.getConfiguration().getId());

    // JDK, Encoding, OS, and Host
    header.add("JDK: " + System.getProperty("java.version") + " (" + System.getProperty("java.vm.info") + ")");
    header.add("OS encoding: " + System.getProperty("file.encoding") + ", Mule encoding: "
        + context.getConfiguration().getDefaultEncoding());
    String patch = System.getProperty("sun.os.patch.level", null);
    header.add("OS: " + System.getProperty("os.name") + (patch != null && !"unknown".equalsIgnoreCase(patch) ? " - " + patch : "")
        + " (" + System.getProperty("os.version") + ", " + System.getProperty("os.arch") + ")");
    try {
      InetAddress host = NetworkUtils.getLocalHost();
      header.add("Host: " + host.getHostName() + " (" + host.getHostAddress() + ")");
    } catch (UnknownHostException e) {
      // ignore
    }

    // Dev/Production mode
    // TODO for now now used, potentially a 'production' mode can disable direcotry (non-api) hot-deployment for tight app control
    // final boolean productionMode = StartupContext.get().getStartupOptions().containsKey("production");
    // header.add("Mode: " + (productionMode ? "Production" : "Development"));

    header.add(" ");
  }

}
