/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.consumer;

import java.io.InputStream;
import java.net.URL;

import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;

import org.xml.sax.InputSource;

public abstract class AbstractInputStreamStrategy implements WsdlRetrieverStrategy
{

    protected Definition getWsdlDefinition(URL url, InputStream response) throws WSDLException
    {
        Definition wsdlDefinition;
        WSDLReader wsdlReader = WSDLFactory.newInstance().newWSDLReader();
        wsdlDefinition = wsdlReader.readWSDL(url.toString(), new InputSource(response));
        return wsdlDefinition;
    }
}
