/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.exception;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.core.api.processor.Processor;

/**
 * Exception thrown when a recursion exists between static flow-refs.
 *
 * @since 4.3.0
 */
public class RecursiveFlowRefException extends LifecycleException {

  private static final long serialVersionUID = 336572098969292323L;

  public RecursiveFlowRefException(String offendingFlowName, Processor flowRefProcessor) {
    super(createStaticMessage("Found a possible infinite recursion involving flows named " + offendingFlowName),
          flowRefProcessor);

  }
}
