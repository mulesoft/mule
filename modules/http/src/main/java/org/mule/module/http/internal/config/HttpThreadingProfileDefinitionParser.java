/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.config;

import static org.mule.module.http.internal.listener.DefaultHttpListenerConfig.DEFAULT_MAX_THREADS;
import org.mule.api.config.ThreadingProfile;
import org.mule.config.spring.parsers.specific.ThreadingProfileDefinitionParser;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Threading profile definition parser for HTTP, that sets a custom default value for the maxThreadsActive property.
 */
public class HttpThreadingProfileDefinitionParser extends ThreadingProfileDefinitionParser
{

    public HttpThreadingProfileDefinitionParser(String propertyName, String defaults)
    {
        super(propertyName, defaults);
    }

    public HttpThreadingProfileDefinitionParser(String propertyName, String defaults, Class<? extends ThreadingProfile> threadingProfileClass)
    {
        super(propertyName, defaults, threadingProfileClass);
    }

    @Override
    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        builder.addPropertyValue("maxThreadsActive", DEFAULT_MAX_THREADS);
        super.parseChild(element, parserContext, builder);
    }
}
