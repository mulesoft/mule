/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor.strategy;

import org.mule.runtime.core.api.MuleContext;

/**
 * Abstract Factory for creating concrete {@link ProcessingStrategy}ies.
 *
 * @since 4.0
 */
public interface ProcessingStrategyFactory {

  /**
   * @return a new {@link ProcessingStrategy}.
   */
  ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix);
}
