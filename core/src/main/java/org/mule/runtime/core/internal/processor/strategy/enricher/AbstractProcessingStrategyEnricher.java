/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.processor.strategy.enricher;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType;

/**
 * An abstract {@link ProcessingStrategyEnricher} that implements basic logic for the chain of responsibility pattern for
 * enriching the reactive processor according to its {@link ProcessingType}.
 * 
 * @since 4.4.0, 4.3.1
 */
public abstract class AbstractProcessingStrategyEnricher implements ProcessingStrategyEnricher {

  @Override
  public ReactiveProcessor enrich(ReactiveProcessor processor) {
    if (getProcessingTypes().contains(processor.getProcessingType())) {
      return doCreateProcessingStrategyChain(processor);
    }

    return nextCustomizer().map(enricher -> enricher
        .enrich(processor))
        .orElseThrow(() -> new MuleRuntimeException(
                                                    I18nMessageFactory
                                                        .createStaticMessage("Could not enrich for processing type "
                                                            + processor.getProcessingType())));
  }

  protected abstract ReactiveProcessor doCreateProcessingStrategyChain(ReactiveProcessor processor);
}
