/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.construct;

import java.util.List;

import org.apache.commons.lang.Validate;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.construct.processor.FlowConstructStatisticsMessageProcessor;
import org.mule.interceptor.LoggingInterceptor;
import org.mule.interceptor.ProcessingTimeInterceptor;
import org.mule.processor.ResponseMessageProcessorAdapter;
import org.mule.processor.StopFurtherMessageProcessingMessageProcessor;
import org.mule.processor.chain.DefaultMessageProcessorChain;

/**
 * A template class for configuration patterns, which takes care of setting common message processors and optional
 * transformers defined on the pattern.
 */
public abstract class AbstractConfigurationPattern extends AbstractFlowConstruct
{
    protected final List<MessageProcessor> transformers;
    protected final List<MessageProcessor> responseTransformers;

    public AbstractConfigurationPattern(String name,
                                        MuleContext muleContext,
                                        List<MessageProcessor> transformers,
                                        List<MessageProcessor> responseTransformers)
    {
        super(name, muleContext);

        Validate.notNull(transformers, "transformers can't be null");
        Validate.notNull(responseTransformers, "transformers can't be null");

        this.transformers = transformers;
        this.responseTransformers = responseTransformers;
    }

    @Override
    protected final void configureMessageProcessors(final MessageProcessorChainBuilder builder)
        throws MuleException
    {
        builder.chain(new ProcessingTimeInterceptor());
        builder.chain(new LoggingInterceptor());
        builder.chain(new FlowConstructStatisticsMessageProcessor());

        configureMessageProcessorsBeforeTransformation(builder);

        builder.chain(DefaultMessageProcessorChain.from(transformers));
        builder.chain(new ResponseMessageProcessorAdapter(
            DefaultMessageProcessorChain.from(responseTransformers)));

        builder.chain(new StopFurtherMessageProcessingMessageProcessor());

        configureMessageProcessorsAfterTransformation(builder);
    }

    public boolean hasTransformers()
    {
        return !transformers.isEmpty();
    }

    public boolean hasResponseTransformers()
    {
        return !responseTransformers.isEmpty();
    }

    protected abstract void configureMessageProcessorsBeforeTransformation(final MessageProcessorChainBuilder builder)
        throws MuleException;

    protected abstract void configureMessageProcessorsAfterTransformation(final MessageProcessorChainBuilder builder)
        throws MuleException;
}
