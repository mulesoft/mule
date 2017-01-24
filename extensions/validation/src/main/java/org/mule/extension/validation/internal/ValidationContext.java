/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.internal;

import org.mule.extension.validation.api.ValidationExtension;
import org.mule.extension.validation.api.ValidationOptions;

/**
 * A context object to gather different components that are necessary for performing a validation
 *
 * @since 3.7.0
 */
public final class ValidationContext {

  private final ValidationMessages messages;
  private final ValidationOptions options;
  private final ValidationExtension config;

  public ValidationContext(ValidationMessages messages, ValidationOptions options) {
    this(messages, options, null);
  }

  public ValidationContext(ValidationOptions options, ValidationExtension config) {
    this(config.getMessageFactory(), options, config);
  }

  public ValidationContext(ValidationMessages messages, ValidationOptions options,
                           ValidationExtension config) {
    this.messages = messages;
    this.options = options;
    this.config = config;
  }

  /**
   * Returns the {@link ValidationMessages} instance to be used when generating feedback messages
   *
   * @return a {@link ValidationMessages}
   */
  public ValidationMessages getMessages() {
    return messages;
  }

  /**
   * Returns the configured {@link ValidationOptions} for this validation
   *
   * @return a {@link ValidationOptions}
   */
  public ValidationOptions getOptions() {
    return options;
  }

  /**
   * Returns the {@link ValidationExtension} with the configuration values
   * 
   * @return a {@link ValidationExtension}
   */
  public ValidationExtension getConfig() {
    return config;
  }
}
