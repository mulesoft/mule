/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.factories.MessageProcessorChainFactoryBean;
import org.mule.config.spring.parsers.delegate.ParentContextDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;

/**
 * This allows a message processor to be defined globally, or embedded within an
 * endpoint.
 */
public class MessageProcessorChainDefinitionParser extends ParentContextDefinitionParser
{
    public MessageProcessorChainDefinitionParser()
    {
        super(MuleOrphanDefinitionParser.ROOT_ELEMENT, new MuleOrphanDefinitionParser(
            MessageProcessorChainFactoryBean.class, false));
        otherwise(new ChildDefinitionParser("messageProcessor", MessageProcessorChainFactoryBean.class));
    }

}
