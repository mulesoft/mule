/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.consumer;

import java.io.InputStream;
import java.net.URL;

import javax.wsdl.WSDLException;

public interface WsdlRetrieverStrategy 
{
	InputStream retrieveWsdl(URL url) throws WSDLException;
}
