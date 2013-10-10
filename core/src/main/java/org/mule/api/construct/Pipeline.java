/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.construct;

import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.ProcessingStrategy;
import org.mule.api.source.MessageSource;

import java.util.List;

/**
 * A pipeline has an ordered list of {@link MessageProcessor}'s that are invoked in order to processor new
 * messages received from it's {@link MessageSource}
 */
public interface Pipeline extends FlowConstruct
{

    public void setMessageSource(MessageSource messageSource);

    public MessageSource getMessageSource();

    public void setMessageProcessors(List<MessageProcessor> messageProcessors);

    public List<MessageProcessor> getMessageProcessors();

    public void setProcessingStrategy(ProcessingStrategy processingStrategy);

    public ProcessingStrategy getProcessingStrategy();
    
}
