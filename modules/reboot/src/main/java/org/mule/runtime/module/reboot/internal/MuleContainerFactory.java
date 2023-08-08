/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.reboot.internal;

import static org.mule.runtime.jpms.api.JpmsUtils.createModuleLayerClassLoader;
import static org.mule.runtime.jpms.api.MultiLevelClassLoaderFactory.MULTI_LEVEL_URL_CLASSLOADER_FACTORY;

import static java.lang.ClassLoader.getSystemClassLoader;
import static java.lang.System.getProperty;
import static java.lang.Thread.currentThread;
import static java.util.ServiceLoader.load;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * A factory for {@link MuleContainer} instances. Responsible for choosing the right implementation class and setting up its
 * {@link ClassLoader}.
 *
 * @since 4.5
 */
public class MuleContainerFactory {

  private static final String CLASSNAME_MULE_CONTAINER = "org.mule.runtime.module.launcher.DefaultMuleContainer";

  private final String muleHomeDirectoryPropertyName;
  private final String muleBaseDirectoryPropertyName;

  public MuleContainerFactory(String muleHomeDirectoryPropertyName, String muleBaseDirectoryPropertyName) {
    this.muleHomeDirectoryPropertyName = muleHomeDirectoryPropertyName;
    this.muleBaseDirectoryPropertyName = muleBaseDirectoryPropertyName;
  }

  /**
   * Creates the {@link MuleContainer} instance.
   *
   * @param args Any arguments to forward to the Container (that have not been yet processed by the bootstrapping application).
   * @return A new {@link MuleContainer} instance.
   * @throws Exception If there is any problem creating the {@link MuleContainer} instance. The bootstrapping application should
   *                   exit immediately.
   */
  public MuleContainer create(String[] args) throws Exception {
    final MuleContainerProvider containerProvider = load(MuleContainerProvider.class).iterator().next();

    ClassLoader originalCl = currentThread().getContextClassLoader();
    ClassLoader muleSystemCl = createContainerSystemClassLoader(lookupMuleHome(), lookupMuleBase());
    currentThread().setContextClassLoader(muleSystemCl);
    try {
      return containerProvider.provide(args);
    } finally {
      currentThread().setContextClassLoader(originalCl);
    }
  }

  /**
   * Creates the Container's {@link ClassLoader} from the given MULE_HOME and MULE_BASE locations.
   *
   * @param muleHome The location of the MULE_HOME directory.
   * @param muleBase The location of the MULE_BASE directory.
   * @return A {@link ClassLoader} suitable for loading the {@link MuleContainer} class and all its dependencies.
   */
  protected ClassLoader createContainerSystemClassLoader(File muleHome, File muleBase) {
    DefaultMuleClassPathConfig config = new DefaultMuleClassPathConfig(muleHome, muleBase);

    return createModuleLayerClassLoader(config.getOptURLs().toArray(new URL[config.getOptURLs().size()]),
                                        config.getMuleURLs().toArray(new URL[config.getMuleURLs().size()]),
                                        MULTI_LEVEL_URL_CLASSLOADER_FACTORY,
                                        getSystemClassLoader());
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
