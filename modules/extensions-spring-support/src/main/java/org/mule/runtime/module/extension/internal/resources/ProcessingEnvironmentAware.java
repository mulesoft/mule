/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
