/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.processor;

import static org.mule.runtime.core.privileged.processor.MessageProcessors.getProcessingStrategy;
import static org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder.newLazyProcessorChainBuilder;

import static java.util.Collections.emptyMap;

import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.processor.chain.AbstractMessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.dsl.api.component.AbstractComponentFactory;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.xml.namespace.QName;

@NoExtend
public class MessageProcessorChainFactoryBean extends AbstractComponentFactory<MessageProcessorChain>
    implements MuleContextAware {

  protected List<Processor> processors;
  protected String name;
  protected MuleContext muleContext;

  @Inject
  protected ConfigurationComponentLocator locator;

  public void setMessageProcessors(List<Processor> processors) {
    this.processors = processors;
  }

  @Override
  public Map<QName, Object> getAnnotations() {
    return emptyMap();
  }

  @Override
  public MessageProcessorChain doGetObject() throws Exception {
    AbstractMessageProcessorChainBuilder builder = getBuilderInstance();
    builder.chain(processors.toArray(Processor[]::new));
    return newLazyProcessorChainBuilder(builder,
                                        muleContext,
                                        () -> getProcessingStrategy(locator, this).orElse(null));
  }

  protected AbstractMessageProcessorChainBuilder getBuilderInstance() {
    return new DefaultMessageProcessorChainBuilder();
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

}
