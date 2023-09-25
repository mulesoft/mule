/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.boot.internal;

import static java.lang.System.getProperty;
import static java.lang.Thread.currentThread;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;

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
   * Creates the Container's {@link ClassLoader} from the given MULE_HOME and MULE_BASE locations.
   *
   * @param muleHome The location of the MULE_HOME directory.
   * @param muleBase The location of the MULE_BASE directory.
   * @return A {@link ClassLoader} suitable for loading the {@link MuleContainer} class and all its dependencies.
   */
  protected abstract ClassLoader createContainerSystemClassLoader(File muleHome, File muleBase);

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
