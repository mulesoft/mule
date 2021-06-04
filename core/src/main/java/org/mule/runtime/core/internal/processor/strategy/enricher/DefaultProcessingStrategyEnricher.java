/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.processor.strategy.enricher;

import com.google.common.collect.ImmutableSet;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType;
import org.mule.runtime.core.internal.management.provider.MuleManagementUtilsProvider;

import java.util.Set;
import java.util.function.Supplier;

import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.*;

/**
 * A default {@link ProcessingStrategyEnricher} enriches as {@link CpuLiteNonBlockingProcessingStrategyEnricher} for every
 * processing type.
 * 
 * @since 4.4.0, 4.3.1
 */
public class DefaultProcessingStrategyEnricher extends CpuLiteNonBlockingProcessingStrategyEnricher {

  public DefaultProcessingStrategyEnricher(ProcessingStrategyEnricher nextEnricher,
                                           MuleManagementUtilsProvider managementUtilsProvider,
                                           Supplier<Scheduler> liteSchedulerProvider) {
    super(nextEnricher, managementUtilsProvider, liteSchedulerProvider);
  }

  @Override
  public Set<ProcessingType> getProcessingTypes() {
    return ImmutableSet.of(CPU_LITE, CPU_LITE_ASYNC, BLOCKING, IO_RW, CPU_INTENSIVE);
  }
}
