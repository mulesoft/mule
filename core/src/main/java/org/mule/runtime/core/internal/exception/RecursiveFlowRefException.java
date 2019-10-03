/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

  private static final long serialVersionUID = 336572098969292321L;

  public RecursiveFlowRefException(String offendingFlowName, Processor flowRefProcessor) {
    super(createStaticMessage("Found a possible infinite recursion involving flows named " + offendingFlowName),
          flowRefProcessor);

  }
}
