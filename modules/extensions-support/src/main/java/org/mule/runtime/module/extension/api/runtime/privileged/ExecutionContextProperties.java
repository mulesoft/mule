/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.runtime.privileged;


import static org.mule.runtime.core.api.config.MuleProperties.PROPERTY_PREFIX;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;

/**
 * Constants used for information related to {@link org.mule.runtime.extension.api.runtime.operation.ExecutionContext}
 *
 * @since 4.1
 */
public class ExecutionContextProperties {

  /**
   * The key of an execution context variable which holds the {@link CompletionCallback} that a non blocking component will use to
   * notify completion or failure
   */
  public static final String COMPLETION_CALLBACK_CONTEXT_PARAM = PROPERTY_PREFIX + "COMPLETION_CALLBACK_CONTEXT_PARAM";

  private ExecutionContextProperties() {}

}
