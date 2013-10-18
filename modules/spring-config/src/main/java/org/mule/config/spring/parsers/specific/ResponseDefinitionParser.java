/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorBuilder;
import org.mule.config.spring.factories.MessageProcessorChainFactoryBean;
import org.mule.config.spring.factories.ResponseMessageProcessorsFactoryBean;
import org.mule.config.spring.parsers.delegate.ParentContextDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;

public class ResponseDefinitionParser extends ParentContextDefinitionParser
{

    public ResponseDefinitionParser()
    {
        super("endpoint", new ChildDefinitionParser("responseMessageProcessor",
            EndpointResponseMessageProcessorChainFactoryBean.class));
        and("inbound-endpoint", new ChildDefinitionParser("responseMessageProcessor",
            EndpointResponseMessageProcessorChainFactoryBean.class));
        and("outbound-endpoint", new ChildDefinitionParser("responseMessageProcessor",
            EndpointResponseMessageProcessorChainFactoryBean.class));
        otherwise(new ChildDefinitionParser("messageProcessor", ResponseMessageProcessorsFactoryBean.class));
    }

    // This is used to avoid additional wrapping of endpoint response message processor which shouldn't even be wrapped to start with
    // The optimal solution is to have response message processors contained inside the <response> element injected into their grandparent element directly.
    private static class EndpointResponseMessageProcessorChainFactoryBean extends MessageProcessorChainFactoryBean
    {
        public Object getObject() throws Exception
        {
            DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
            builder.setName("processor chain '" + name + "'");
            for (Object processor : processors)
            {
                if (processor instanceof MessageProcessor)
                {
                    builder.chain((MessageProcessor) processor);
                }
                else if (processor instanceof MessageProcessorBuilder)
                {
                    builder.chain((MessageProcessorBuilder) processor);
                }
                else
                {
                    throw new IllegalArgumentException(
                        "MessageProcessorBuilder should only have MessageProcessor's or MessageProcessorBuilder's configured");
                }
            }
            return builder.build();
        }
    }

}
