/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.http.config;

import org.mule.config.spring.parsers.collection.ChildListDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.specific.ComplexComponentDefinitionParser;
import org.mule.providers.http.HttpConnector;
import org.mule.providers.http.components.RestServiceWrapper;
import org.mule.providers.http.transformers.HttpClientMethodResponseToObject;
import org.mule.providers.http.transformers.HttpResponseToString;
import org.mule.providers.http.transformers.ObjectToHttpClientMethodRequest;
import org.mule.providers.http.transformers.UMOMessageToHttpResponse;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Reigsters a Bean Definition Parser for handling <code><http:connector></code> elements.
 */
public class HttpNamespaceHandler extends NamespaceHandlerSupport
{

    public void init()
    {
        registerBeanDefinitionParser("connector", new MuleOrphanDefinitionParser(HttpConnector.class, true));
        
        registerBeanDefinitionParser("rest-service-component", new ComplexComponentDefinitionParser(RestServiceWrapper.class));
        registerBeanDefinitionParser("payloadParameterNames", new ChildListDefinitionParser("payloadParameterNames"));
        registerBeanDefinitionParser("requiredParams", new ChildMapEntryDefinitionParser("requiredParams","key","value"));
        registerBeanDefinitionParser("optionalParams", new ChildMapEntryDefinitionParser("optionalParams","key","value"));
        
        registerBeanDefinitionParser("http-client-response-to-object", new MuleOrphanDefinitionParser(HttpClientMethodResponseToObject.class, false));
        registerBeanDefinitionParser("http-response-to-string", new MuleOrphanDefinitionParser(HttpResponseToString.class, false));
        registerBeanDefinitionParser("object-to-http-client-request", new MuleOrphanDefinitionParser(ObjectToHttpClientMethodRequest.class, false));
        registerBeanDefinitionParser("message-to-http-response", new MuleOrphanDefinitionParser(UMOMessageToHttpResponse.class, false));
    }
}
