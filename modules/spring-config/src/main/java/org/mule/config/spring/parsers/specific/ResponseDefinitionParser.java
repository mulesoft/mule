/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.factories.MessageProcessorChainFactoryBean;
import org.mule.config.spring.factories.ResponseMessageProcessorsFactoryBean;
import org.mule.config.spring.parsers.delegate.ParentContextDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;

public class ResponseDefinitionParser extends ParentContextDefinitionParser
{

    public ResponseDefinitionParser()
    {
        super("endpoint", new ChildDefinitionParser("responseMessageProcessor",
            MessageProcessorChainFactoryBean.class));
        and("inbound-endpoint", new ChildDefinitionParser("responseMessageProcessor",
            MessageProcessorChainFactoryBean.class));
        and("outbound-endpoint", new ChildDefinitionParser("responseMessageProcessor",
            MessageProcessorChainFactoryBean.class));
        otherwise(new ChildDefinitionParser("messageProcessor", ResponseMessageProcessorsFactoryBean.class));
    }

}
