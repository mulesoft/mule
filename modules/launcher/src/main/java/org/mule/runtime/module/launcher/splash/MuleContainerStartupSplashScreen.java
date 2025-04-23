/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.splash;

import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.container.api.MuleFoldersUtil.ARTIFACT_PATCHES_FOLDER;
import static org.mule.runtime.container.api.MuleFoldersUtil.getArtifactPatchesLibFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getPatchesLibFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getServerPluginsFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getServicesFolder;
import static org.mule.runtime.manifest.api.MuleManifest.getMuleManifest;

import static java.lang.String.format;
import static java.lang.System.getProperties;
import static java.lang.System.getProperty;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Arrays.asList;
import static java.util.Arrays.sort;
import static java.util.stream.Collectors.toMap;

import static org.apache.commons.io.IOUtils.lineIterator;
import static org.apache.commons.lang3.StringUtils.defaultString;

import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.api.util.NetworkUtils;
import org.mule.runtime.core.internal.util.splash.SplashScreen;
import org.mule.runtime.module.tls.internal.util.SecurityUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import org.apache.commons.io.LineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MuleContainerStartupSplashScreen extends SplashScreen {

  private Logger LOGGER = LoggerFactory.getLogger(MuleContainerStartupSplashScreen.class);
  private final List<String> additionalSplashEntries;

  /**
   * @param additionalSplashEntries Additional splash entries to include in the body.
   */
  public MuleContainerStartupSplashScreen(List<String> additionalSplashEntries) {
    this.additionalSplashEntries = additionalSplashEntries;
  }

  public void doBody() {

    try (InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("banner.txt")) {
      LineIterator lineIterator = lineIterator(resourceAsStream, defaultCharset());
      while (lineIterator.hasNext()) {
        doBody(lineIterator.nextLine());
      }
    } catch (IOException e) {
      LOGGER.warn("Could not create banner");
    }

    String notset = CoreMessages.notSet().getMessage();

    // Mule Version, Timestamp, and Server ID
    Manifest mf = getMuleManifest().getManifest();
    Attributes att = mf.getMainAttributes();
    if (att.values().size() > 0) {
      doBody(defaultString(getMuleManifest().getProductDescription(), notset));
      doBody(format("%s Build: %s", CoreMessages.version().getMessage(),
                    defaultString(getMuleManifest().getBuildNumber(), notset)));

      doBody(defaultString(getMuleManifest().getVendorName(), notset));
      doBody(defaultString(getMuleManifest().getProductMoreInfo(), notset));
    } else {
      doBody(CoreMessages.versionNotSet().getMessage());
    }
    doBody(" ");

    // TODO maybe be more precise and count from container bootstrap time?
    doBody(CoreMessages.serverStartedAt(System.currentTimeMillis()).getMessage());

    doBody(format("JDK: %s (%s)", getProperty("java.version"), getProperty("java.vm.info")));
    listJavaSystemProperties();

    String patch = getProperty("sun.os.patch.level", null);

    doBody(format("OS: %s%s (%s, %s)", getProperty("os.name"),
                  (patch != null && !"unknown".equalsIgnoreCase(patch) ? " - " + patch : ""),
                  getProperty("os.version"), getProperty("os.arch")));

    // Additional entries
    for (String additionalEntry : additionalSplashEntries) {
      doBody(additionalEntry);
    }

    try {
      InetAddress host = NetworkUtils.getLocalHost();
      doBody(format("Host: %s (%s)", host.getHostName(), host.getHostAddress()));
    } catch (UnknownHostException e) {
      // ignore
    }
    if (!SecurityUtils.isDefaultSecurityModel()) {
      doBody("Security model: " + SecurityUtils.getSecurityModel());
    }
    if (RUNTIME_VERBOSE_PROPERTY.isEnabled()) {
      listServicesIfPresent();
      listServerPluginsIfPresent();
      listPatchesIfPresent();
      listMuleSystemProperties();
    }
  }

  private void listServerPluginsIfPresent() {
    File serverPluginsFolder = getServerPluginsFolder();
    if (serverPluginsFolder != null && serverPluginsFolder.exists()) {
      listItems(asList(serverPluginsFolder.list()), "Mule server plugins:");
    }
  }

  private void listServicesIfPresent() {
    File servicesDirectory = getServicesFolder();
    if (servicesDirectory != null && servicesDirectory.exists()) {
      listItems(asList(servicesDirectory.list()), "Mule services:");
    }
  }

  private void listPatchesIfPresent() {
    listPatchesIn(getPatchesLibFolder(), "Applied patches:", (dir, name) -> !ARTIFACT_PATCHES_FOLDER.equals(name));
    listPatchesIn(getArtifactPatchesLibFolder(), "Applied artifact patches:", (dir, name) -> true);
  }

  private void listPatchesIn(File patchesDirectory, String description, FilenameFilter filenameFilter) {
    if (patchesDirectory != null && patchesDirectory.exists()) {
      String[] patches = patchesDirectory.list(filenameFilter);
      sort(patches);
      listItems(asList(patches), description);
    }
  }

  private void listMuleSystemProperties() {
    Map<String, String> muleProperties = new HashMap<>();
    getProperties().stringPropertyNames().stream().filter(property -> property.startsWith(SYSTEM_PROPERTY_PREFIX))
        .forEach(property -> muleProperties.put(property, getProperty(property)));
    listItems(muleProperties, "Mule system properties:");
  }

  private void listJavaSystemProperties() {
    listItems(Stream.of("java.vendor", "java.vm.name", "java.home")
        .collect(toMap(String::toString, System::getProperty)), "JDK properties:");
  }
}
