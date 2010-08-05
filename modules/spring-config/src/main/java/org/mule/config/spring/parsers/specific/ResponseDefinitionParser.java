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

import org.mule.config.spring.factories.ResponseMessageProcessorsFactoryBean;
import org.mule.config.spring.parsers.delegate.ParentContextDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.ParentDefinitionParser;

public class ResponseDefinitionParser extends ParentContextDefinitionParser
{

    public ResponseDefinitionParser()
    {
        super("flow", new ChildDefinitionParser("messageProcessor",
            ResponseMessageProcessorsFactoryBean.class));
        //otherwise(new ParentDefinitionParser());
    }

}
