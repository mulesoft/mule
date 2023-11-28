/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.log4j.internal;

import static org.mule.runtime.api.util.MuleSystemProperties.MULE_LOG_SEPARATION_DISABLED;

import static java.lang.System.getProperty;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.module.log4j.boot.api.MuleLog4jContextFactory;

import org.apache.logging.log4j.core.selector.ContextSelector;

/**
 * Utility class used to set the corresponding {@link ContextSelector} to a {@link MuleLog4jContextFactory}, depending on the
 * value of the system property {@code mule.disableLogSeparation}. It's here instead of be part of {@link MuleLog4jContextFactory}
 * because that class will be in lib/boot, and the implementations of {@link ContextSelector} are in modules of lib/mule.
 *
 * @since 4.5.0
 */
public final class MuleLog4jConfiguratorUtils {

  private MuleLog4jConfiguratorUtils() {
    // private constructor to avoid wrong instantiations
  }

  /**
   * Depending on the system property {@code mule.disableLogSeparation} parameter, it sets an {@link ArtifactAwareContextSelector}
   * or a {@link SimpleContextSelector} to the given {@code contextFactory}.
   * 
   * @param contextFactory the {@link MuleLog4jContextFactory} where the selector will be set.
   */
  public static void configureSelector(MuleLog4jContextFactory contextFactory) {
    configureSelector(contextFactory, getProperty(MULE_LOG_SEPARATION_DISABLED) == null, false);
  }

  /**
   * Depending on the given {@code logSeparationEnabled} parameter, it sets an {@link ArtifactAwareContextSelector} or a
   * {@link SimpleContextSelector} to the given {@code contextFactory}.
   *
   * @param contextFactory       the {@link MuleLog4jContextFactory} where the selector will be set.
   * @param logSeparationEnabled boolean determining which context selector should be used.
   */
  public static void configureSelector(MuleLog4jContextFactory contextFactory, boolean logSeparationEnabled) {
    configureSelector(contextFactory, logSeparationEnabled, false);
  }

  /**
   * Depending on the given {@code logSeparationEnabled} parameter, it sets an {@link ArtifactAwareContextSelector} or a
   * {@link SimpleContextSelector} to the given {@code contextFactory}.
   * 
   * @param contextFactory       the {@link MuleLog4jContextFactory} where the selector will be set.
   * @param logSeparationEnabled boolean determining which context selector should be used.
   * @param singleAppMode        boolean determining if the container is in single app mode.
   */
  public static void configureSelector(MuleLog4jContextFactory contextFactory, boolean logSeparationEnabled,
                                       boolean singleAppMode) {
    if (singleAppMode) {
      contextFactory.setContextSelector(new SingleAppModeContextSelector(), MuleLog4jConfiguratorUtils::disposeIfDisposable);
    } else if (logSeparationEnabled) {
      contextFactory.setContextSelector(new ArtifactAwareContextSelector(), MuleLog4jConfiguratorUtils::disposeIfDisposable);
    } else {
      contextFactory.setContextSelector(new SimpleContextSelector(), MuleLog4jConfiguratorUtils::disposeIfDisposable);
    }
  }

  private static void disposeIfDisposable(ContextSelector selector) {
    if (selector instanceof Disposable) {
      ((Disposable) selector).dispose();
    }
  }
}
