/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.construct;

import org.mule.runtime.core.api.processor.MessageProcessorContainer;
import org.mule.runtime.core.api.processor.ProcessingDescriptor;
import org.mule.runtime.core.api.processor.ProcessingStrategy;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.strategy.factory.ProcessingStrategyFactory;

import java.util.List;

/**
 * A pipeline has an ordered list of {@link Processor}'s that are invoked in order to processor new messages received from it's
 * {@link MessageSource}
 */
public interface Pipeline extends FlowConstruct, MessageProcessorContainer, ProcessingDescriptor, MessageProcessorPathResolver {

  public void setMessageSource(MessageSource messageSource);

  public MessageSource getMessageSource();

  public void setMessageProcessors(List<Processor> messageProcessors);

  public List<Processor> getMessageProcessors();

  public void setProcessingStrategyFactory(ProcessingStrategyFactory processingStrategyFactory);

  public ProcessingStrategy getProcessingStrategy();

}
