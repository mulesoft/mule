/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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

    header.add(" ");
  }

}
