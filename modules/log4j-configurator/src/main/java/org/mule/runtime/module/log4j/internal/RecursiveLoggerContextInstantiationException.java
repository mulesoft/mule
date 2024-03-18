/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
