/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher;

import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;
import static org.mule.runtime.core.util.Preconditions.checkArgument;

import org.mule.runtime.core.util.StringUtils;

import java.io.File;

/**
 * Calculates folders for a mule server based on the
 * {@value org.mule.runtime.core.api.config.MuleProperties#MULE_HOME_DIRECTORY_PROPERTY} property
 */
public class MuleFoldersUtil {

  public static final String EXECUTION_FOLDER = ".mule";
  public static final String LIB_FOLDER = "lib";
  public static final String APPS_FOLDER = "apps";
  public static final String PLUGINS_FOLDER = "plugins";
  public static final String CLASSES_FOLDER = "classes";
  public static final String PER_APP_LIB_FOLDER = "per-app";
  public static final String DOMAINS_FOLDER = "domains";
  public static final String CONTAINER_APP_PLUGINS = "plugins";
  public static final String USER_FOLDER = "user";
  public static final String SERVICES_FOLDER = "services";


  private MuleFoldersUtil() {}

  public static File getMuleHomeFolder() {
    String muleHome = System.getProperty(MULE_HOME_DIRECTORY_PROPERTY, ".");

    return new File(muleHome);
  }

  /**
   * @return a {@link File} pointing to the container folder that contains services.
   */
  public static File getServicesFolder() {
    return new File(getMuleHomeFolder(), SERVICES_FOLDER);
  }

  /**
   * Returns the file for a given service name.
   *
   * @param name name of the service. Non empty.
   * @return a {@link File} pointing to the folder that corresponds to the provided service name when installed.
   */
  public static File getServiceFolder(String name) {
    checkArgument(!StringUtils.isEmpty(name), "name cannot be empty");
    return new File(getServicesFolder(), name);
  }

  public static File getDomainsFolder() {
    return new File(getMuleHomeFolder(), DOMAINS_FOLDER);
  }

  public static File getDomainFolder(String domainName) {
    return new File(getDomainsFolder(), domainName);
  }

  public static File getDomainLibFolder(String domainName) {
    return new File(getDomainFolder(domainName), LIB_FOLDER);
  }

  public static File getAppsFolder() {
    return new File(getMuleHomeFolder(), APPS_FOLDER);
  }

  public static File getAppFolder(String appName) {
    return new File(getAppsFolder(), appName);
  }

  public static File getAppClassesFolder(String appName) {
    return new File(getAppFolder(appName), CLASSES_FOLDER);
  }

  public static File getAppLibFolder(String appName) {
    return new File(getAppFolder(appName), LIB_FOLDER);
  }

  public static File getAppPluginsFolder(String appName) {
    return new File(getAppFolder(appName), PLUGINS_FOLDER);
  }

  public static File getAppSharedPluginLibsFolder(String appName) {
    return new File(getAppPluginsFolder(appName), LIB_FOLDER);
  }

  public static File getExecutionFolder() {
    return new File(getMuleHomeFolder(), EXECUTION_FOLDER);
  }

  public static File getMuleLibFolder() {
    return new File(getMuleHomeFolder(), LIB_FOLDER);
  }

  public static File getUserLibFolder() {
    return new File(getMuleLibFolder(), USER_FOLDER);
  }

  public static File getMulePerAppLibFolder() {
    return new File(getMuleLibFolder(), PER_APP_LIB_FOLDER);
  }

  public static File getAppTempFolder(String appName) {
    return new File(getExecutionFolder(), appName);
  }

  public static File getContainerAppPluginsFolder() {
    return new File(getMuleHomeFolder(), CONTAINER_APP_PLUGINS);
  }

  /**
   * @return a {@link File} pointing to the container folder used to temporarily store services on deployment
   */
  public static File getServicesTempFolder() {
    return new File(getExecutionFolder(), SERVICES_FOLDER);
  }
}
