/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.config;

import org.mule.module.http.internal.HttpParamType;
import org.mule.module.http.internal.HttpSingleParam;

/**
 * Namespace handler for functional tests of this module. It adds the bean definition parser for the header element
 * which is otherwise added by the HttpNamespaceHandler form the HTTP transport (outside tests).
 */
public class HttpTestNamespaceHandler extends HttpNamespaceHandler
{

    public void init()
    {
        registerBeanDefinitionParser("header", new HttpMessageSingleParamDefinitionParser(HttpSingleParam.class, HttpParamType.HEADER));
        registerBeanDefinitionParser("response-builder", new HttpResponseBuilderDefinitionParser("responseBuilder"));
        registerBeanDefinitionParser("error-response-builder", new HttpResponseBuilderDefinitionParser("errorResponseBuilder"));
        super.init();
    }
}
