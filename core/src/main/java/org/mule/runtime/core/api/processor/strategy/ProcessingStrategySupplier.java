/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor.strategy;

/**
 * Provides access to the actual {@link ProcessingStrategy} used in a construct.
 *
 * @since 4.3
 */
public interface ProcessingStrategySupplier {

  ProcessingStrategy getProcessingStrategy();

}
