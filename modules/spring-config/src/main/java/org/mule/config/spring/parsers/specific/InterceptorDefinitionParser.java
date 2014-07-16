/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.api.interceptor.Interceptor;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;

import org.w3c.dom.Element;

/**
 * This allows a interceptor to be defined on a global interceptor stack or on a service.
 */
public class InterceptorDefinitionParser extends ChildDefinitionParser
{

    public static final String INTERCEPTOR = "interceptor";

    public InterceptorDefinitionParser(Class interceptor)
    {
        super(INTERCEPTOR, interceptor, Interceptor.class);
    }

    /**
     * For custom transformers
     */
    public InterceptorDefinitionParser()
    {
        super(INTERCEPTOR, null, Interceptor.class);
    }

    @Override
    public String getPropertyName(Element e)
    {
        String parentName = e.getParentNode().getLocalName().toLowerCase();
        if ("flow".equals(parentName) || "inbound".equals(parentName) || "endpoint".equals(parentName)
            || "inbound-endpoint".equals(parentName) || "outbound-endpoint".equals(parentName)
            || "async-reply".equals(parentName) || "processor-chain".equals(parentName))
        {
            return "messageProcessor";
        }
        else
        {
            return super.getPropertyName(e);

        }
    }
}
