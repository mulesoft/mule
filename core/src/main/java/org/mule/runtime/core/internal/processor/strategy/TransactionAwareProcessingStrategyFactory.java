/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.processor.strategy;

import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;

/**
 * Marker interface for {@link ProcessingStrategyFactory}'s that create {@link ProcessingStrategy}'s that are transaction aware
 * and will not fail when a transaction is active. Typically this will be supported by forcing single-threaded behaviour when a
 * transaction is present.
 *
 * @since 4.0
 */
public interface TransactionAwareProcessingStrategyFactory extends ProcessingStrategyFactory {

}
