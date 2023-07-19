/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
