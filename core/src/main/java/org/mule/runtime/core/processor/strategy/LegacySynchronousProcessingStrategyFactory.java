/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.transaction.TransactionCoordination.isTransactionActive;
import static reactor.core.Exceptions.propagate;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.exception.MessagingException;

import java.util.function.Consumer;

/**
 * This processing strategy processes all message processors in the calling thread.
 */
@Deprecated
public class LegacySynchronousProcessingStrategyFactory implements ProcessingStrategyFactory {

  static ProcessingStrategy LEGACY_SYNCHRONOUS_PROCESSING_STRATEGY_INSTANCE = new AbstractLegacyProcessingStrategy() {

    @Override
    public boolean isSynchronous() {
      return true;
    }

    protected Consumer<Event> createOnEventConsumer() {
      return event -> {
      };
    }
  };

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    return LEGACY_SYNCHRONOUS_PROCESSING_STRATEGY_INSTANCE;
  }

}
