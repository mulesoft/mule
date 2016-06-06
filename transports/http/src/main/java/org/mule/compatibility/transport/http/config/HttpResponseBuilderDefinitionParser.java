/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.config;

import org.mule.runtime.config.spring.parsers.delegate.ParentContextDefinitionParser;
import org.mule.runtime.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.runtime.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.runtime.module.http.internal.listener.HttpResponseBuilder;

public class HttpResponseBuilderDefinitionParser extends ParentContextDefinitionParser
{
    public HttpResponseBuilderDefinitionParser(String setterMethod)
    {
        super("listener", new ChildDefinitionParser(setterMethod, HttpResponseBuilder.class));
        and("mule", new MuleOrphanDefinitionParser(HttpResponseBuilder.class, true));
        otherwise(new MessageProcessorDefinitionParser(org.mule.compatibility.transport.http.components.HttpResponseBuilder.class));
    }

}