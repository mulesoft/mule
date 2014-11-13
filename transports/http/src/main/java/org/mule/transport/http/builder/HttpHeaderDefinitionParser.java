/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.builder;

import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.delegate.ParentContextDefinitionParser;
import org.mule.module.http.internal.HttpParamType;
import org.mule.module.http.internal.HttpSingleParam;
import org.mule.module.http.internal.config.HttpRequestSingleParamDefinitionParser;

/**
 * Custom bean definition parser for the "header" element that works for both the old "header" element in the
 * HTTP transport (inside the response builder) and the new "header" element in the HTTP module (inside the
 * request builder).
 */
public class HttpHeaderDefinitionParser extends ParentContextDefinitionParser
{
    public HttpHeaderDefinitionParser()
    {
        super("response-builder", new ChildMapEntryDefinitionParser("headers", "name", "value"));
        otherwise(new HttpRequestSingleParamDefinitionParser(HttpSingleParam.class, HttpParamType.HEADER));
    }

}
