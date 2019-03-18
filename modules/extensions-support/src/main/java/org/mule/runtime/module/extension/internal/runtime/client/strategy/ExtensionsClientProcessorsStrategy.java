/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client.strategy;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.client.OperationParameters;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessor;

/**
 * Interface that provides help for executing extension operations.
 *
 * @since 4.1.6
 */
@NoImplement
public interface ExtensionsClientProcessorsStrategy {

  /**
   * Returns the appropiate {@link OperationMessageProcessor} to be used for executing the operation.
   *
   * @param extensionName the name of the extension to run the operation.
   * @param operationName the name of the operation to run.
   * @param parameters the operation parameters used to run the operation
   */
  OperationMessageProcessor getOperationMessageProcessor(String extensionName, String operationName,
                                                         OperationParameters parameters);

  /**
   * Returns the appropiate event to be used to run de desired operation with the {@link OperationParameters} given.
   *
   * @param parameters the {@link OperationParameters} used to execute the operation.
   * @return
   */
  CoreEvent getEvent(OperationParameters parameters);

  /**
   * Signal that the {@link OperationMessageProcessor} has been finished using and depending on the implementation it may end up
   * freeing its resources, or it may do it in the future.
   *
   * @param operationMessageProcessor processor to be disposed.
   */
  void disposeProcessor(OperationMessageProcessor operationMessageProcessor);

}
