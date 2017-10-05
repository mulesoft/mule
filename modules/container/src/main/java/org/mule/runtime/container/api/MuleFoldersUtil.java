/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.container.api;

import static java.io.File.separator;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.internal.util.StandaloneServerUtils.getMuleBase;
import static org.mule.runtime.core.internal.util.StandaloneServerUtils.getMuleHome;
import org.mule.runtime.core.api.util.StringUtils;

import java.io.File;

/**
 * Calculates folders for a mule server based on the
 * {@value org.mule.runtime.core.api.config.MuleProperties#MULE_HOME_DIRECTORY_PROPERTY} property
 */
public class MuleFoldersUtil {

  public static final String EXECUTION_FOLDER = ".mule";
  public static final String LIB_FOLDER = "lib";
  public static final String SHARED_FOLDER = "shared";
  public static final String APPS_FOLDER = "apps";
  public static final String PLUGINS_FOLDER = "plugins";
  public static final String DOMAINS_FOLDER = "domains";
  public static final String CONTAINER_APP_PLUGINS = "plugins";
  public static final String SERVER_PLUGINS = "server-plugins";
  public static final String CONF = "conf";
  public static final String USER_FOLDER = "user";
  public static final String PATCHES_FOLDER = "patches";
  public static final String SERVICES_FOLDER = "services";


  private MuleFoldersUtil() {}

  /**
   * @return the mule runtime installation folder.
   */
  public static File getMuleHomeFolder() {
    File muleHome = getMuleHome().orElse(null);
    if (muleHome == null) {
      muleHome = new File(".");
    }
    return muleHome;
  }

  /**
   * @return the mule runtime base folder.
   */
  public static File getMuleBaseFolder() {
    File muleBase = getMuleBase().orElse(null);
    if (muleBase == null) {
      muleBase = getMuleHomeFolder();
    }
    return muleBase;
  }

  /**
   * @return a {@link File} pointing to the container folder that contains services.
   */
  public static File getServicesFolder() {
    return new File(getMuleBaseFolder(), SERVICES_FOLDER);
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

  /**
   * @return a {@link File} pointing to the folder where server plugins are located.
   */
  public static File getServerPluginsFolder() {
    return new File(getMuleBaseFolder(), SERVER_PLUGINS);
  }

  /**
   * @return a {@link File} pointing to the folder where the server configuration is located.
   */
  public static File getConfFolder() {
    return new File(getMuleBaseFolder(), CONF);
  }

  public static File getDomainsFolder() {
    return new File(getMuleBaseFolder(), DOMAINS_FOLDER);
  }

  public static File getDomainFolder(String domainName) {
    return new File(getDomainsFolder(), domainName);
  }

  public static File getDomainLibFolder(String domainName) {
    return new File(getDomainFolder(domainName), LIB_FOLDER);
  }

  public static File getAppsFolder() {
    return new File(getMuleBaseFolder(), APPS_FOLDER);
  }

  public static File getAppFolder(String appName) {
    return new File(getAppsFolder(), appName);
  }

  public static File getAppDataFolder(String appDataFolder) {
    return new File(getExecutionFolder(), appDataFolder);
  }

  /**
   * @param appName name of the application to look for
   * @return the libraries folder in the deployed application with the given name
   */
  public static File getAppLibFolder(String appName) {
    return new File(getAppFolder(appName), getAppLibsFolderPath());
  }

  /**
   * @param appName name of the application to look for
   * @return the plugins folder in the deployed application with the given name
   */
  public static File getAppPluginsFolder(String appName) {
    return new File(getAppFolder(appName), getAppPluginsFolderPath());
  }

  /**
   * @return relative path for plugins on an application
   */
  public static String getAppPluginsFolderPath() {
    return PLUGINS_FOLDER + separator;
  }

  /**
   * @param appName name of the application to look for
   * @return the shared libraries folder in the deployed application with the given name
   */
  public static File getAppSharedLibsFolder(String appName) {
    return new File(getAppFolder(appName), getAppSharedLibsFolderPath());
  }

  /**
   * @return relative path for shared libraries on an application
   */
  public static String getAppSharedLibsFolderPath() {
    return getAppLibsFolderPath() + SHARED_FOLDER + separator;
  }

  private static String getAppLibsFolderPath() {
    return LIB_FOLDER + separator;
  }

  /**
   * @return relative path for libraries on an application
   */
  public static File getExecutionFolder() {
    return new File(getMuleBaseFolder(), EXECUTION_FOLDER);
  }

  public static File getMuleLibFolder() {
    return new File(getMuleHomeFolder(), LIB_FOLDER);
  }

  public static File getUserLibFolder() {
    return new File(getMuleLibFolder(), USER_FOLDER);
  }

  /**
   * @return directory where the patches are placed in the runtime
   */
  public static File getPatchesLibFolder() {
    return new File(getMuleLibFolder(), PATCHES_FOLDER);
  }

  public static File getContainerAppPluginsFolder() {
    return new File(getMuleBaseFolder(), CONTAINER_APP_PLUGINS);
  }

  /**
   * @return a {@link File} pointing to the container folder used to temporarily store services on deployment
   */
  public static File getServicesTempFolder() {
    return new File(getExecutionFolder(), SERVICES_FOLDER);
  }
}
