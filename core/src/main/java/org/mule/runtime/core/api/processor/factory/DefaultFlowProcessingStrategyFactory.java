/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor.factory;

import org.mule.runtime.core.api.processor.ProcessingStrategy;
import org.mule.runtime.core.construct.flow.DefaultFlowProcessingStrategy;


public class DefaultFlowProcessingStrategyFactory implements ProcessingStrategyFactory {

  @Override
  public ProcessingStrategy create() {
    return new DefaultFlowProcessingStrategy();
  }

}
