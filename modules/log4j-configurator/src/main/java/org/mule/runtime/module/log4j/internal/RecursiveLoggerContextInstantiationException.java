/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.log4j.internal;

/**
 * Indicates that the instantiation of a {@link MuleLoggerContext} is not possible due to the same context being already under
 * construction (logging during the {@link MuleLoggerContext} construction triggers this recursive instantiation)
 *
 * @since 4.5
 */
public class RecursiveLoggerContextInstantiationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public RecursiveLoggerContextInstantiationException(String message) {
    super(message);
  }
}
