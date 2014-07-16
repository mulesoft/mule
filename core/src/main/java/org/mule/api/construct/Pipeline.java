/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.construct;

import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorContainer;
import org.mule.api.processor.ProcessingDescriptor;
import org.mule.api.processor.ProcessingStrategy;
import org.mule.api.source.MessageSource;

import java.util.List;

/**
 * A pipeline has an ordered list of {@link MessageProcessor}'s that are invoked in order to processor new
 * messages received from it's {@link MessageSource}
 */
public interface Pipeline extends FlowConstruct, MessageProcessorContainer, ProcessingDescriptor, MessageProcessorPathResolver
{

    public void setMessageSource(MessageSource messageSource);

    public MessageSource getMessageSource();

    public void setMessageProcessors(List<MessageProcessor> messageProcessors);

    public List<MessageProcessor> getMessageProcessors();

    public void setProcessingStrategy(ProcessingStrategy processingStrategy);

    public ProcessingStrategy getProcessingStrategy();

}
