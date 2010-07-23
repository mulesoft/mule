/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.MuleDefinitionParser;
import org.mule.config.spring.parsers.delegate.ParentContextDefinitionParser;
import org.mule.config.spring.parsers.generic.ParentDefinitionParser;

/**
 * Handles a reference to a globla Message Processor.
 * e.g., <message-processor ref="doSomething" />
 */
public class MessageProcessorRefDefinitionParser extends ParentContextDefinitionParser
{
    public MessageProcessorRefDefinitionParser()
    {
        super("response", 
            addAlias(new ParentDefinitionParser(), AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF, "responseMessageProcessor"));
        and("endpoint", 
            addAlias(new ParentDefinitionParser(), AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF, "messageProcessor"));
        and("inbound-endpoint", 
            addAlias(new ParentDefinitionParser(), AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF, "messageProcessor"));
        and("outbound-endpoint", 
            addAlias(new ParentDefinitionParser(), AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF, "messageProcessor"));
        otherwise(
            addAlias(new ParentDefinitionParser(), AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF, "transformer"));
    }

    private static MuleDefinitionParser addAlias(MuleDefinitionParser parser, String alias, String name)
    {
        parser.addAlias(alias, name);
        return parser;
    }
}
