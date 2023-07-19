/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.construct;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.processor.ProcessingDescriptor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.api.source.MessageSource;

import java.util.List;

/**
 * A pipeline has an ordered list of {@link Processor}'s that are invoked in order to processor new messages received from it's
 * {@link MessageSource}
 */
@NoImplement
public interface Pipeline extends FlowConstruct, ProcessingDescriptor {

  /**
   * @return source of messages to use.
   */
  MessageSource getSource();

  /**
   * @return processors to execute on a {@link Message}.
   */
  List<Processor> getProcessors();

  /**
   * @return the maximum concurrency to be used by the {@link Pipeline}.
   */
  int getMaxConcurrency();

  /**
   * @return the {@link ProcessingStrategyFactory} applied to this Pipeline.
   */
  ProcessingStrategyFactory getProcessingStrategyFactory();
}
