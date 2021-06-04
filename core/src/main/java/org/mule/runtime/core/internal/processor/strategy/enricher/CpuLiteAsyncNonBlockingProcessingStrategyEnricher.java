/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.processor.strategy.enricher;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.management.provider.MuleManagementUtilsProvider;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import static com.google.common.collect.ImmutableSet.of;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE_ASYNC;

/**
 * A {@link ProcessingStrategyEnricher} for CPU_LITE processing type.
 *
 * @since 4.4.0, 4.3.1
 */
public class CpuLiteAsyncNonBlockingProcessingStrategyEnricher extends NonBlockingProcessingStrategyEnricher {

  public CpuLiteAsyncNonBlockingProcessingStrategyEnricher(ProcessingStrategyEnricher nextEnricher,
                                                           MuleManagementUtilsProvider managementUtilsProvider,
                                                           Supplier<Scheduler> liteSchedulerProvider,
                                                           Supplier<ScheduledExecutorService> nonBlockingSchedulerSupplier) {
    super(nextEnricher, managementUtilsProvider, liteSchedulerProvider, nonBlockingSchedulerSupplier);
  }

  @Override
  public Set<ReactiveProcessor.ProcessingType> getProcessingTypes() {
    return of(CPU_LITE_ASYNC);
  }
}
