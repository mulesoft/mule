/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.boot.internal;

import static org.mule.runtime.jpms.api.JpmsUtils.createModuleLayerClassLoader;
import static org.mule.runtime.jpms.api.MultiLevelClassLoaderFactory.MULTI_LEVEL_URL_CLASSLOADER_FACTORY;
import static org.mule.runtime.module.boot.internal.util.SystemUtils.getCommandLineOptions;

import static java.lang.ClassLoader.getSystemClassLoader;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static java.lang.Thread.currentThread;
import static java.util.ServiceLoader.load;

import org.mule.runtime.module.boot.api.MuleContainer;
import org.mule.runtime.module.boot.api.MuleContainerProvider;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

/**
 * A base class for implementing {@link MuleContainerFactory}.
 *
 * @since 4.6
 */
public class DefaultMuleContainerFactory implements MuleContainerFactory {

  public static final String[][] CLI_OPTIONS = {
      {"builder", "true", "Configuration Builder Type"},
      {"config", "true", "Configuration File"},
      {"idle", "false", "Whether to run in idle (unconfigured) mode"},
      {"main", "true", "Main Class"},
      {"mode", "true", "Run Mode"},
      {"props", "true", "Startup Properties"},
      {"production", "false", "Production Mode"},
      {"debug", "false", "Configure Mule for JPDA remote debugging."},
      {"app", "true", "Application to start"}
  };

  static final String APP_COMMAND_LINE_OPTION = "app";
  static final String DEPLOYMENT_APPLICATION_PROPERTY = "mule.deploy.applications";
  static final String INVALID_DEPLOY_APP_CONFIGURATION_ERROR =
      format("Cannot set both '%s' option and '%s' property", APP_COMMAND_LINE_OPTION, DEPLOYMENT_APPLICATION_PROPERTY);

  private final String muleHomeDirectoryPropertyName;
  private final String muleBaseDirectoryPropertyName;

  public DefaultMuleContainerFactory(String muleHomeDirectoryPropertyName, String muleBaseDirectoryPropertyName) {
    this.muleHomeDirectoryPropertyName = muleHomeDirectoryPropertyName;
    this.muleBaseDirectoryPropertyName = muleBaseDirectoryPropertyName;
  }

  @Override
  public final MuleContainer create(String[] args) throws Exception {
    validateCommnadLineOptions(args);

    return createMuleContainer();
  }

  private void validateCommnadLineOptions(String[] args) {
    Map<String, Object> commandlineOptions = getCommandLineOptions(args, CLI_OPTIONS);
    String appOption = (String) commandlineOptions.get(APP_COMMAND_LINE_OPTION);
    if (appOption != null) {
      if (getProperty(DEPLOYMENT_APPLICATION_PROPERTY) != null) {
        throw new IllegalArgumentException(INVALID_DEPLOY_APP_CONFIGURATION_ERROR);
      }
      setProperty(DEPLOYMENT_APPLICATION_PROPERTY, appOption);
    }
  }

  MuleContainer createMuleContainer() throws IOException, Exception {
    ClassLoader muleSystemCl = createContainerSystemClassLoader(lookupMuleHome(), lookupMuleBase());

    final MuleContainerProvider containerProvider = load(MuleContainerProvider.class, muleSystemCl).iterator().next();

    ClassLoader originalCl = currentThread().getContextClassLoader();
    currentThread().setContextClassLoader(muleSystemCl);
    try {
      return containerProvider.provide();
    } finally {
      currentThread().setContextClassLoader(originalCl);
    }
  }

  /**
   * @param muleHome The location of the MULE_HOME directory.
   * @param muleBase The location of the MULE_BASE directory.
   * @return The set of JAR Urls located under Mule home folder.
   */
  private DefaultMuleClassPathConfig createMuleClassPathConfig(File muleHome, File muleBase) {
    return new DefaultMuleClassPathConfig(muleHome, muleBase);
  }

  ClassLoader createContainerSystemClassLoader(File muleHome, File muleBase) {
    DefaultMuleClassPathConfig config = createMuleClassPathConfig(muleHome, muleBase);

    return createModuleLayerClassLoader(config.getOptURLs().toArray(new URL[config.getOptURLs().size()]),
                                        config.getMuleURLs().toArray(new URL[config.getMuleURLs().size()]),
                                        MULTI_LEVEL_URL_CLASSLOADER_FACTORY,
                                        new URLClassLoader(config.getResourceURLs()
                                            .toArray(new URL[config.getResourceURLs().size()]),
                                                           getSystemClassLoader()));
  }

  private File lookupMuleHome() throws IOException {
    return lookupMuleDirectoryLocation(muleHomeDirectoryPropertyName, "%MULE_HOME%");
  }

  private File lookupMuleBase() throws IOException {
    return lookupMuleDirectoryLocation(muleBaseDirectoryPropertyName, "%MULE_BASE%");
  }

  private File lookupMuleDirectoryLocation(String muleDirectoryLocationPropertyName, String placeholderForNullValue)
      throws IOException {
    File muleBase = null;
    String muleBaseVar = getProperty(muleDirectoryLocationPropertyName);

    if (muleBaseVar != null && !muleBaseVar.trim().equals("") && !muleBaseVar.equals(placeholderForNullValue)) {
      muleBase = new File(muleBaseVar).getCanonicalFile();
    }

    validateMuleDirectoryLocation(muleDirectoryLocationPropertyName, muleBase);
    return muleBase;
  }

  private void validateMuleDirectoryLocation(String muleDirectoryLocationPropertyName, File muleDirectoryLocation)
      throws IllegalArgumentException {
    if (muleDirectoryLocation == null) {
      throw new IllegalArgumentException("The system property " + muleDirectoryLocationPropertyName
          + " is not set.");
    }
    if (!muleDirectoryLocation.exists()) {
      throw new IllegalArgumentException("The system property " + muleDirectoryLocationPropertyName
          + " does not contain a valid directory (" + muleDirectoryLocation.getAbsolutePath() + ").");
    }
    if (!muleDirectoryLocation.isDirectory()) {
      throw new IllegalArgumentException("The system property " + muleDirectoryLocationPropertyName
          + " does not contain a valid directory (" + muleDirectoryLocation.getAbsolutePath() + ").");
    }
  }
}
