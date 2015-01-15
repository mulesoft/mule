/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig.parsers.specific;

import org.mule.module.springconfig.factories.MessageProcessorChainFactoryBean;
import org.mule.module.springconfig.factories.ResponseMessageProcessorsFactoryBean;
import org.mule.module.springconfig.parsers.delegate.ParentContextDefinitionParser;
import org.mule.module.springconfig.parsers.generic.ChildDefinitionParser;

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
