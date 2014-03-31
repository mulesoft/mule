/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.construct;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.processor.DynamicPipeline;
import org.mule.api.processor.DynamicPipelineException;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.config.i18n.CoreMessages;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.processor.chain.AbstractMessageProcessorChain;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.processor.chain.SimpleMessageProcessorChain;
import org.mule.util.StringUtils;
import org.mule.util.UUID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DynamicPipelineMessageProcessor extends AbstractInterceptingMessageProcessor implements DynamicPipeline
{

    private String pipelineId;
    private AbstractMessageProcessorChain preChain;
    private AbstractMessageProcessorChain postChain;
    private MessageProcessor staticChain;
    private Flow flow;

    public DynamicPipelineMessageProcessor(Flow flow)
    {
        this.flow = flow;
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        return processNext(event);
    }

    @Override
    public void setListener(MessageProcessor next)
    {
        if (staticChain == null)
        {
            if (next instanceof InterceptingMessageProcessor)
            {
                //wrap with chain to avoid intercepting the postChain
                staticChain = new SimpleMessageProcessorChain(next);
            }
            else
            {
                staticChain = next;
            }
        }
        super.setListener(next);
    }

    @Override
    public String resetAndUpdatePipeline(String id, List<MessageProcessor> preMessageProcessors, List<MessageProcessor> postMessageProcessors) throws MuleException
    {
        checkPipelineId(id);

        //build new dynamic chains
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder(flow);
        builder.chain(preMessageProcessors);

        builder.chain(staticChain);
        builder.chain(postMessageProcessors);
        MessageProcessorChain newChain = builder.build();

        Lifecycle preChainOld = preChain;
        Lifecycle postChainOld = postChain;
        preChain = new SimpleMessageProcessorChain(preMessageProcessors);
        postChain = new SimpleMessageProcessorChain(postMessageProcessors);
        initDynamicChains();

        //hook chain as last step to avoid synchronization
        super.setListener(newChain);

        //dispose old dynamic chains
        disposeDynamicChains(preChainOld, postChainOld);

        return getPipelineId();
    }

    private synchronized void checkPipelineId(String id) throws DynamicPipelineException
    {
        if (!StringUtils.equals(pipelineId, id))
        {
            throw new DynamicPipelineException(CoreMessages.createStaticMessage("Invalid Dynamic Pipeline ID"));
        }
        if (pipelineId == null && id == null)
        {
            pipelineId = UUID.getUUID();
        }
    }

    private synchronized String getPipelineId()
    {
        return pipelineId;
    }

    @Override
    public String resetPipeline(String id) throws MuleException
    {
        List<MessageProcessor> emptyList = new ArrayList<MessageProcessor>();
        return resetAndUpdatePipeline(id, emptyList, emptyList);
    }

    @Override
    public DynamicPipeline injectBefore(MessageProcessor... messageProcessors)
    {
        return new Builder(this).injectBefore(messageProcessors);
    }

    @Override
    public DynamicPipeline injectAfter(MessageProcessor... messageProcessors)
    {
        return new Builder(this).injectAfter(messageProcessors);
    }

    @Override
    public String resetAndUpdatePipeline(String id) throws MuleException
    {
        return resetPipeline(id);
    }

    private void initDynamicChains() throws MuleException
    {
        for (Lifecycle chain : new Lifecycle[] {preChain, postChain})
        {
            if (chain != null)
            {
                flow.injectFlowConstructMuleContext(chain);
                flow.initialiseIfInitialisable(chain);
                flow.startIfStartable(chain);
            }
        }
    }

    private void disposeDynamicChains(Lifecycle... chains) throws MuleException
    {
        for (Lifecycle chain : chains)
        {
            if (chain != null)
            {
                chain.stop();
                chain.dispose();
            }
        }
    }

    private static class Builder implements DynamicPipeline
    {
        private DynamicPipelineMessageProcessor dynamicPipelineMessageProcessor;
        private List<MessageProcessor> preList = new ArrayList<MessageProcessor>();
        private List<MessageProcessor> postList = new ArrayList<MessageProcessor>();

        private Builder(DynamicPipelineMessageProcessor dynamicPipelineMessageProcessor)
        {
            this.dynamicPipelineMessageProcessor = dynamicPipelineMessageProcessor;
        }

        @Override
        public String resetAndUpdatePipeline(String id, List<MessageProcessor> preMessageProcessors, List<MessageProcessor> postMessageProcessors) throws MuleException
        {
            return dynamicPipelineMessageProcessor.resetAndUpdatePipeline(id, preMessageProcessors, postMessageProcessors);
        }

        @Override
        public String resetPipeline(String id) throws MuleException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public DynamicPipeline injectBefore(MessageProcessor... messageProcessors)
        {
            Collections.addAll(preList, messageProcessors);
            return this;
        }

        @Override
        public DynamicPipeline injectAfter(MessageProcessor... messageProcessors)
        {
            Collections.addAll(postList, messageProcessors);
            return this;
        }

        @Override
        public String resetAndUpdatePipeline(String id) throws MuleException
        {
            return dynamicPipelineMessageProcessor.resetAndUpdatePipeline(id, preList, postList);
        }
    }
}
