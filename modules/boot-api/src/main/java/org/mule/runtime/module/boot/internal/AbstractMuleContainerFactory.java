/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.boot.internal;

import static org.mule.runtime.jpms.api.JpmsUtils.createModuleLayerClassLoader;
import static org.mule.runtime.jpms.api.MultiLevelClassLoaderFactory.MULTI_LEVEL_URL_CLASSLOADER_FACTORY;

import static java.lang.ClassLoader.getSystemClassLoader;
import static java.lang.System.getProperty;
import static java.lang.Thread.currentThread;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;

/**
 * A base class for implementing {@link MuleContainerFactory}.
 *
 * @since 4.6
 */
public abstract class AbstractMuleContainerFactory implements MuleContainerFactory {

  private static final String CLASSNAME_MULE_CONTAINER = "org.mule.runtime.module.launcher.DefaultMuleContainer";

  private final String muleHomeDirectoryPropertyName;
  private final String muleBaseDirectoryPropertyName;

  public AbstractMuleContainerFactory(String muleHomeDirectoryPropertyName, String muleBaseDirectoryPropertyName) {
    this.muleHomeDirectoryPropertyName = muleHomeDirectoryPropertyName;
    this.muleBaseDirectoryPropertyName = muleBaseDirectoryPropertyName;
  }

  @Override
  public MuleContainer create(String[] args) throws Exception {
    ClassLoader muleSystemCl = createContainerSystemClassLoader(lookupMuleHome(), lookupMuleBase());

    Class<?> muleClass = muleSystemCl.loadClass(CLASSNAME_MULE_CONTAINER);
    Constructor<?> c = muleClass.getConstructor(String[].class);

    ClassLoader originalCl = currentThread().getContextClassLoader();
    currentThread().setContextClassLoader(muleSystemCl);
    try {
      // the cast to Object is to disambiguate the fact that the String array must be passed as a single argument instead of
      // having
      // them unpacked for the varargs method newInstance
      return (MuleContainer) c.newInstance((Object) args);
    } finally {
      currentThread().setContextClassLoader(originalCl);
    }
  }

  /**
   * @param muleHome The location of the MULE_HOME directory.
   * @param muleBase The location of the MULE_BASE directory.
   * @return The set of JAR Urls located under Mule home folder.
   */
  protected abstract DefaultMuleClassPathConfig createMuleClassPathConfig(File muleHome, File muleBase);

  private ClassLoader createContainerSystemClassLoader(File muleHome, File muleBase) {
    DefaultMuleClassPathConfig config = createMuleClassPathConfig(muleHome, muleBase);

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
