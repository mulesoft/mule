/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.log4j.internal;

import static org.mule.runtime.api.util.MuleSystemProperties.MULE_LOG_SEPARATION_DISABLED;

import static java.lang.System.getProperty;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.module.log4j.boot.api.MuleLog4jContextFactory;

import org.apache.logging.log4j.core.selector.ContextSelector;

public final class MuleLog4jConfiguratorUtils {

  private MuleLog4jConfiguratorUtils() {
    // private constructor to avoid wrong instantiations
  }

  public static MuleLog4jContextFactory createContextFactory(boolean logSeparationEnabled) {
    MuleLog4jContextFactory contextFactory = new MuleLog4jContextFactory();
    configureSelector(contextFactory, logSeparationEnabled);
    return contextFactory;
  }

  public static void configureSelector(MuleLog4jContextFactory contextFactory) {
    configureSelector(contextFactory, getProperty(MULE_LOG_SEPARATION_DISABLED) == null);
  }

  public static void configureSelector(MuleLog4jContextFactory contextFactory, boolean logSeparationEnabled) {
    if (logSeparationEnabled) {
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
