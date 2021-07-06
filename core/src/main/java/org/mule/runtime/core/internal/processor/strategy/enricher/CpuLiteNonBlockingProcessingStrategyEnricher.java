/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.processor.strategy.enricher;

import static org.mule.runtime.core.internal.processor.strategy.reactor.builder.ComponentProcessingStrategyReactiveProcessorBuilder.processingStrategyReactiveProcessorFrom;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.diagnostics.DiagnosticsService;
import org.mule.runtime.core.api.processor.ReactiveProcessor;

import java.util.function.Supplier;

/**
 * A {@link ReactiveProcessorEnricher} for CPU_LITE processing type.
 *
 * @since 4.4.0
 */
public class CpuLiteNonBlockingProcessingStrategyEnricher implements ReactiveProcessorEnricher {

  private final Supplier<Scheduler> liteSchedulerSupplier;
  private final DiagnosticsService diagnosticsService;
  private final MuleContext muleContext;

  public CpuLiteNonBlockingProcessingStrategyEnricher(Supplier<Scheduler> liteSchedulerSupplier,
                                                      DiagnosticsService diagnosticsService,
                                                      MuleContext muleContext) {
    this.liteSchedulerSupplier = liteSchedulerSupplier;
    this.diagnosticsService = diagnosticsService;
    this.muleContext = muleContext;
  }

  @Override
  public ReactiveProcessor enrich(ReactiveProcessor processor) {
    return processingStrategyReactiveProcessorFrom(processor, liteSchedulerSupplier.get(), muleContext)
        .withDiagnosticsService(diagnosticsService)
        .build();
  }
}
