/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.reboot.internal;

import static java.lang.String.format;
import static java.lang.System.getProperty;

/**
 * Helps with the creation and provisioning of the {@link MuleContainerWrapper} implementation instance as a singleton.
 * <p>
 * Only thread-safe for threads started after the first provisioning.
 *
 * @since 4.5
 */
public class MuleContainerWrapperProvider {

  private static final String MULE_BOOTSTRAP_CONTAINER_WRAPPER_CLASS_SYSTEM_PROPERTY = "mule.bootstrap.container.wrapper.class";
  private static final String DEFAULT_WRAPPER_IMPL_CLASS = "org.mule.runtime.module.tanuki.internal.MuleContainerTanukiWrapper";
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

  /**
   * Creates the implementation instance based on the system property
   * {@link #MULE_BOOTSTRAP_CONTAINER_WRAPPER_CLASS_SYSTEM_PROPERTY}.
   *
   * @return The {@link MuleContainerWrapper} implementation.
   */
  private static MuleContainerWrapper createContainerWrapper() {
    String wrapperClassName = getProperty(MULE_BOOTSTRAP_CONTAINER_WRAPPER_CLASS_SYSTEM_PROPERTY, DEFAULT_WRAPPER_IMPL_CLASS);
    try {
      Class<?> wrapperClass = MuleContainerWrapper.class.getClassLoader().loadClass(wrapperClassName);
      if (!MuleContainerWrapper.class.isAssignableFrom(wrapperClass)) {
        throw new RuntimeException(format("System property '%s' does not define an implementation of %s",
                                          MuleContainerWrapper.class.getName(),
                                          wrapperClassName));
      }
      return (MuleContainerWrapper) wrapperClass.getConstructor().newInstance();
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(format("Unable to instantiate MuleContainerWrapper '%s'", wrapperClassName), e);
    }
  }
}
