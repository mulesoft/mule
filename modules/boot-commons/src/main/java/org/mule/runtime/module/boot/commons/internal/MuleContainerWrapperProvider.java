/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.boot.commons.internal;

import static java.lang.String.format;
import static java.lang.System.getProperty;

import org.mule.runtime.module.boot.api.MuleContainerLifecycleWrapper;
import org.mule.runtime.module.boot.api.MuleContainerLifecycleWrapperProvider;

/**
 * Helps with the creation and provisioning of the {@link MuleContainerWrapper} implementation instance as a singleton.
 * <p>
 * Only thread-safe for threads started after the first provisioning.
 *
 * @since 4.5
 */
public class MuleContainerWrapperProvider implements MuleContainerLifecycleWrapperProvider {

  private static final String MULE_BOOTSTRAP_CONTAINER_WRAPPER_CLASS_SYSTEM_PROPERTY = "mule.bootstrap.container.wrapper.class";
  private static MuleContainerWrapper INSTANCE;

  /**
   * Creates the implementation instance based on the system property
   * {@link #MULE_BOOTSTRAP_CONTAINER_WRAPPER_CLASS_SYSTEM_PROPERTY}.
   *
   * @return The {@link MuleContainerWrapper} implementation.
   */
  public static MuleContainerWrapper getMuleContainerWrapper() {
    if (INSTANCE == null) {
      INSTANCE = createContainerWrapper();
    }

    return INSTANCE;
  }

  @Override
  public MuleContainerLifecycleWrapper getMuleContainerLifecycleWrapper() {
    getMuleContainerWrapper();
    return INSTANCE;
  }

  /**
   * Creates the implementation instance based on the system property
   * {@link #MULE_BOOTSTRAP_CONTAINER_WRAPPER_CLASS_SYSTEM_PROPERTY}.
   *
   * @return The {@link MuleContainerWrapper} implementation.
   */
  private static MuleContainerWrapper createContainerWrapper() {
    String wrapperClassName = getProperty(MULE_BOOTSTRAP_CONTAINER_WRAPPER_CLASS_SYSTEM_PROPERTY);
    if (wrapperClassName == null) {
      throw new RuntimeException(format("System property '%s' is not defined, it must be set with an implementation of %s",
                                        MULE_BOOTSTRAP_CONTAINER_WRAPPER_CLASS_SYSTEM_PROPERTY,
                                        MuleContainerWrapper.class.getName()));
    }

    try {
      Class<?> wrapperClass = MuleContainerWrapper.class.getClassLoader().loadClass(wrapperClassName);
      if (!MuleContainerWrapper.class.isAssignableFrom(wrapperClass)) {
        throw new RuntimeException(format("System property '%s=%s' does not define an implementation of %s",
                                          MULE_BOOTSTRAP_CONTAINER_WRAPPER_CLASS_SYSTEM_PROPERTY,
                                          wrapperClassName,
                                          MuleContainerWrapper.class.getName()));
      }
      return (MuleContainerWrapper) wrapperClass.getConstructor().newInstance();
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(format("Unable to instantiate MuleContainerWrapper implementation '%s' from system property '%s'",
                                        wrapperClassName,
                                        MULE_BOOTSTRAP_CONTAINER_WRAPPER_CLASS_SYSTEM_PROPERTY),
                                 e);
    }
  }
}
