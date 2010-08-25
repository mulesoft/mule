/*
 * $Id: MessageProcessorDefinitionParser.java 17725 2010-06-25 20:12:28Z tcarlson $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.factories.CompositeMessageProcessorFactoryBean;
import org.mule.config.spring.parsers.delegate.ParentContextDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;

/**
 * This allows a message processor to be defined globally, or embedded within an
 * endpoint.
 */
public class CompositeMessageProcessorDefinitionParser extends ParentContextDefinitionParser
{
    public CompositeMessageProcessorDefinitionParser()
    {
        super(MuleOrphanDefinitionParser.ROOT_ELEMENT, new MuleOrphanDefinitionParser(
            CompositeMessageProcessorFactoryBean.class, false));
        otherwise(new ChildDefinitionParser("messageProcessor", CompositeMessageProcessorFactoryBean.class));
    }

}
