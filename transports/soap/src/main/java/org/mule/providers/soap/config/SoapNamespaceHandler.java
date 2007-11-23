/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.soap.config;

import org.mule.config.spring.parsers.specific.TransformerDefinitionParser;
import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.providers.soap.transformers.HttpRequestToSoapRequest;

/**
 * Reigsters a Bean Definition Parser for handling soap transport elements.
 */
public class SoapNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public void init()
    {
        registerBeanDefinitionParser("transformer-http-to-soap-request", new TransformerDefinitionParser(HttpRequestToSoapRequest.class));
    }

}