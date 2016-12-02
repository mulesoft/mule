/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.transaction.TransactionCoordination.isTransactionActive;
import static reactor.core.Exceptions.propagate;
import static reactor.core.publisher.Flux.from;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import reactor.core.publisher.TopicProcessor;
import reactor.core.scheduler.Schedulers;

/**
 * Creates {@link ReactorProcessingStrategy} instances. This processing strategy demultiplexes incoming messages using the
 * cpu-light scheduler.
 *
 * This processing strategy is not suitable for transactional flows and will fail if used with an active transaction.
 *
 * @since 4.0
 */
public class MultiReactorProcessingStrategyFactory extends ReactorProcessingStrategyFactory {

  @Override
  public ProcessingStrategy create(MuleContext muleContext) {
    return new ReactorProcessingStrategy(() -> muleContext.getSchedulerService().cpuLightScheduler(),
                                         scheduler -> scheduler.stop(muleContext.getConfiguration().getShutdownTimeout(),
                                                                     MILLISECONDS),
                                         muleContext);
  }

}
