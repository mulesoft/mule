/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.construct;

import static org.mule.runtime.core.api.processor.MessageProcessors.newChain;
import static reactor.core.publisher.Flux.from;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.processor.DynamicPipeline;
import org.mule.runtime.core.api.processor.DynamicPipelineBuilder;
import org.mule.runtime.core.api.processor.DynamicPipelineException;
import org.mule.runtime.core.api.processor.InterceptingMessageProcessor;
import org.mule.runtime.core.api.processor.InternalMessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.processor.AbstractInterceptingMessageProcessor;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.core.util.UUID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

/**
 * Experimental implementation that supports a single dynamic pipeline due to restrictions imposed by intercepting message
 * processors and their lifecycle.
 *
 * If more than one client tries to use the functionality the 2nd one will fail due to pipeline ID verification.
 */
public class DynamicPipelineMessageProcessor extends AbstractInterceptingMessageProcessor
    implements DynamicPipeline, InternalMessageProcessor {

  private String pipelineId;
  private MessageProcessorChain preChain;
  private MessageProcessorChain postChain;
  private Processor staticChain;
  private Flow flow;

  public DynamicPipelineMessageProcessor(Flow flow) {
    this.flow = flow;
  }

  @Override
  public Event process(Event event) throws MuleException {
    return processNext(event);
  }

  @Override
  public Publisher<Event> apply(Publisher<Event> publisher) {
    return from(publisher).transform(applyNext());
  }

  @Override
  public void setListener(Processor next) {
    if (staticChain == null) {
      if (next instanceof InterceptingMessageProcessor) {
        // wrap with chain to avoid intercepting the postChain
        staticChain = event -> next.process(event);
      } else {
        staticChain = next;
      }
    }
    super.setListener(next);
  }

  private String resetAndUpdatePipeline(String id, List<Processor> preMessageProcessors,
                                        List<Processor> postMessageProcessors)
      throws MuleException {
    checkPipelineId(id);

    // build new dynamic chains
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(preMessageProcessors);

    builder.chain(staticChain);
    builder.chain(postMessageProcessors);
    MessageProcessorChain newChain = builder.build();
    newChain.setFlowConstruct(flowConstruct);
    newChain.setMuleContext(muleContext);

    Lifecycle preChainOld = preChain;
    Lifecycle postChainOld = postChain;
    preChain = newChain(preMessageProcessors);
    postChain = newChain(postMessageProcessors);
    initDynamicChains();

    // hook chain as last step to avoid synchronization
    super.setListener(newChain);

    // dispose old dynamic chains
    disposeDynamicChains(preChainOld, postChainOld);

    return getPipelineId();
  }

  private synchronized void checkPipelineId(String id) throws DynamicPipelineException {
    if (pipelineId != null) {
      if (!StringUtils.equals(pipelineId, id)) {
        throw new DynamicPipelineException(CoreMessages.createStaticMessage("Invalid Dynamic Pipeline ID"));
      }
    } else {
      pipelineId = (id != null) ? id : UUID.getUUID();
    }
  }

  private synchronized String getPipelineId() {
    return pipelineId;
  }

  private String resetPipeline(String id) throws MuleException {
    List<Processor> emptyList = new ArrayList<>();
    return resetAndUpdatePipeline(id, emptyList, emptyList);
  }

  private void initDynamicChains() throws MuleException {
    for (Lifecycle chain : new Lifecycle[] {preChain, postChain}) {
      if (chain != null) {
        flow.injectFlowConstructMuleContext(chain);
        flow.injectExceptionHandler(chain);
        flow.initialiseIfInitialisable(chain);
        flow.startIfStartable(chain);
      }
    }
  }

  private void disposeDynamicChains(Lifecycle... chains) throws MuleException {
    for (Lifecycle chain : chains) {
      if (chain != null) {
        chain.stop();
        chain.dispose();
      }
    }
  }

  @Override
  public DynamicPipelineBuilder dynamicPipeline(String id) throws DynamicPipelineException {
    checkPipelineId(id);
    return new Builder();
  }

  private class Builder implements DynamicPipelineBuilder {

    private List<Processor> preList = new ArrayList<>();
    private List<Processor> postList = new ArrayList<>();

    @Override
    public DynamicPipelineBuilder injectBefore(Processor... messageProcessors) {
      Collections.addAll(preList, messageProcessors);
      return this;
    }

    @Override
    public DynamicPipelineBuilder injectBefore(List<Processor> messageProcessors) {
      return injectBefore(messageProcessors.toArray(new Processor[messageProcessors.size()]));
    }

    @Override
    public DynamicPipelineBuilder injectAfter(Processor... messageProcessors) {
      Collections.addAll(postList, messageProcessors);
      return this;
    }

    @Override
    public DynamicPipelineBuilder injectAfter(List<Processor> messageProcessors) {
      return injectAfter(messageProcessors.toArray(new Processor[messageProcessors.size()]));
    }

    @Override
    public String resetAndUpdate() throws MuleException {
      return pipeline().resetAndUpdatePipeline(pipeline().getPipelineId(), preList, postList);
    }

    @Override
    public String reset() throws MuleException {
      return pipeline().resetPipeline(pipeline().getPipelineId());
    }

    private DynamicPipelineMessageProcessor pipeline() {
      return DynamicPipelineMessageProcessor.this;
    }

  }
}
