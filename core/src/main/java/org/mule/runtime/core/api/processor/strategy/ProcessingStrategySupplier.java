/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
