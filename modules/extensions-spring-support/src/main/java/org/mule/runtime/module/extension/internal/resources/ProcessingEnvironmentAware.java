/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources;

import javax.annotation.processing.ProcessingEnvironment;

/**
 * Interface to mark a class that {@link ProcessingEnvironment} can be injected
 *
 * @since 4.1
 */
public interface ProcessingEnvironmentAware {

  /**
   * Sets the {@link ProcessingEnvironment}
   *
   * @param processingEnvironment processing environment to inject
   */
  void setProcessingEnvironment(ProcessingEnvironment processingEnvironment);
}
