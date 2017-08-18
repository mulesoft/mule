/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.construct;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.processor.strategy.DirectProcessingStrategyFactory.DIRECT_PROCESSING_STRATEGY_INSTANCE;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.lifecycle.LifecycleStateEnabled;
import org.mule.runtime.core.api.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.source.MessageSource;

import java.util.Optional;

/**
 * A uniquely identified {@link FlowConstruct} that once implemented and configured defines a construct through which messages are
 * processed using {@link MessageSource} and {@link Processor} building blocks.
 */
public interface FlowConstruct extends NamedObject, LifecycleStateEnabled, AnnotatedObject {

  /**
   * @return The exception listener that will be used to handle exceptions that may be thrown at different points during the
   *         message flow defined by this construct.
   */
  MessagingExceptionHandler getExceptionListener();

  /**
   * @return The statistics holder used by this flow construct to keep track of its activity.
   */
  FlowConstructStatistics getStatistics();

  /**
   * @return This muleContext that this flow construct belongs to and runs in the context of.
   */
  MuleContext getMuleContext();

  /**
   * Generate a unique ID string
   */
  String getUniqueIdString();

  /**
   * @return the id of the running mule server
   */
  String getServerId();

  /**
   * @return the {@link ProcessingStrategy} used.
   */
  default ProcessingStrategy getProcessingStrategy() {
    return DIRECT_PROCESSING_STRATEGY_INSTANCE;
  }

  static FlowConstruct getFromAnnotatedObject(ConfigurationComponentLocator componentLocator, AnnotatedObject annotatedObject) {
    Optional<AnnotatedObject> objectFoundOptional =
        componentLocator.find(Location.builder().globalName(annotatedObject.getRootContainerName()).build());
    Optional<FlowConstruct> flowConstruct = objectFoundOptional.flatMap(objectFound -> objectFound instanceof FlowConstruct
        ? of((FlowConstruct) objectFound) : empty()).filter(object -> object != null);
    if (flowConstruct.isPresent()) {
      return flowConstruct.get();
    }
    throw new MuleRuntimeException(createStaticMessage(format(
                                                              "Couldn't find FlowConstruct with global name %s or it was not an instance of FlowConstruct",
                                                              annotatedObject.getRootContainerName())));
  }
}
