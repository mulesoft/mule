/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.consumer.wsdl.strategy.factory;

import org.mule.module.ws.consumer.URLRetrieverStrategy;
import org.mule.module.ws.consumer.WsdlRetrieverStrategy;

import javax.wsdl.WSDLException;

/**
 * Factory to create URLWSDLRetrievers
 */
public class URLWSDLRetrieverStrategyFactory implements WSDLRetrieverStrategyFactory
{

    @Override
    public WsdlRetrieverStrategy createWSDLRetrieverStrategy() throws WSDLException
    {
        return new URLRetrieverStrategy();
    }

}
