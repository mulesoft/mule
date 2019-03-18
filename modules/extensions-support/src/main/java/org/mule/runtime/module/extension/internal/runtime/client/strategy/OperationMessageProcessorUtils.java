/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client.strategy;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessor;

/**
 * This class provides utility for handling {@link OperationMessageProcessor} instances.
 */
public class OperationMessageProcessorUtils {

  /**
   * Disposes the given {@link OperationMessageProcessor}
   *
   * @param processor the processor to be dispoed
   */
  public static void disposeProcessor(OperationMessageProcessor processor) {
    if (processor == null) {
      return;
    }
    try {
      processor.stop();
      processor.dispose();
    } catch (MuleException e) {
      throw new MuleRuntimeException(createStaticMessage("Error while disposing the executing operation"), e);
    }
  }

}
