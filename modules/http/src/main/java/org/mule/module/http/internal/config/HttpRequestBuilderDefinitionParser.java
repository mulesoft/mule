/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.config;

import org.mule.config.spring.parsers.delegate.ParentContextDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.module.http.internal.request.HttpRequesterRequestBuilder;

public class HttpRequestBuilderDefinitionParser extends ParentContextDefinitionParser
{
    public HttpRequestBuilderDefinitionParser()
    {
        super("request", new ChildDefinitionParser("request-builder", HttpRequesterRequestBuilder.class));
        otherwise(new MuleOrphanDefinitionParser(HttpRequesterRequestBuilder.class, true));
    }

}