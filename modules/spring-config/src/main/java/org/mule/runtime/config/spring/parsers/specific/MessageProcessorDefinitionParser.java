/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.delegate.ParentContextDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;

/**
 * This allows a message processor to be defined globally, or embedded within an endpoint.
 * (as either a normal or response processor).
 */
public class MessageProcessorDefinitionParser extends ParentContextDefinitionParser
{

    public MessageProcessorDefinitionParser(Class messageProcessor)
    {
        super(MuleOrphanDefinitionParser.ROOT_ELEMENT, new MuleOrphanDefinitionParser(messageProcessor, false));
        otherwise(new ChildDefinitionParser("messageProcessor", messageProcessor));
    }

    /**
     * For custom processors
     */
    public MessageProcessorDefinitionParser()
    {
        super(MuleOrphanDefinitionParser.ROOT_ELEMENT, new MuleOrphanDefinitionParser(false));
        otherwise(new ChildDefinitionParser("messageProcessor"));
    }

}
