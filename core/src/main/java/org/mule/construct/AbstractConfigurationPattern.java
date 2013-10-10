/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.construct;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.api.processor.ProcessingStrategy;
import org.mule.construct.processor.FlowConstructStatisticsMessageProcessor;
import org.mule.interceptor.ProcessingTimeInterceptor;
import org.mule.lifecycle.processor.ProcessIfStartedMessageProcessor;
import org.mule.processor.ResponseMessageProcessorAdapter;
import org.mule.processor.StopFurtherMessageProcessingMessageProcessor;
import org.mule.processor.chain.DefaultMessageProcessorChain;

import java.util.List;

import org.apache.commons.lang.Validate;

/**
 * A template class for configuration patterns, which takes care of setting common message processors and
 * optional transformers defined on the pattern.
 */
public abstract class AbstractConfigurationPattern extends AbstractPipeline
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
    protected final void configureMessageProcessors(final MessageProcessorChainBuilder builder) throws MuleException
    {
        configureMessageProcessorsBeforeTransformation(builder);

        builder.chain(DefaultMessageProcessorChain.from(transformers));
        builder.chain(new ResponseMessageProcessorAdapter(
            DefaultMessageProcessorChain.from(responseTransformers)));

        builder.chain(new StopFurtherMessageProcessingMessageProcessor());

        configureMessageProcessorsAfterTransformation(builder);
    }
    
    @Override
    protected void configurePreProcessors(MessageProcessorChainBuilder builder) throws MuleException
    {
        super.configurePreProcessors(builder);
        builder.chain(new ProcessIfPipelineStartedMessageProcessor());
        builder.chain(new ProcessingTimeInterceptor());
        builder.chain(new FlowConstructStatisticsMessageProcessor());
    }
    
    public boolean hasTransformers()
    {
        return !transformers.isEmpty();
    }

    public boolean hasResponseTransformers()
    {
        return !responseTransformers.isEmpty();
    }

    @Override
    public final void setProcessingStrategy(ProcessingStrategy processingStrategy)
    {
        throw new UnsupportedOperationException();
    }

    protected abstract void configureMessageProcessorsBeforeTransformation(final MessageProcessorChainBuilder builder)
        throws MuleException;

    protected abstract void configureMessageProcessorsAfterTransformation(final MessageProcessorChainBuilder builder)
        throws MuleException;
}
