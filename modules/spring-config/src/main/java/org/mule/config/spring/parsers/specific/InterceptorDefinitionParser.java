/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
