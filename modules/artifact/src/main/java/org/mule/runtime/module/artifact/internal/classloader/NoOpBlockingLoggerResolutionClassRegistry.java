/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.internal.classloader;

import org.mule.runtime.module.artifact.api.classloader.BlockingLoggerResolutionClassRegistry;

/**
 * A no-op {@link BlockingLoggerResolutionClassRegistry}.
 */
public class NoOpBlockingLoggerResolutionClassRegistry implements BlockingLoggerResolutionClassRegistry {

  @Override
  public void registerClassNeedingBlockingLoggerResolution(Class<?> loggerClass) {
    // Nothing to do
  }

}
