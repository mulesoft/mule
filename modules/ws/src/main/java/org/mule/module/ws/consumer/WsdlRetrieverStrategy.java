/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.consumer;

import javax.wsdl.Definition;
import javax.wsdl.WSDLException;

/**
 * A strategy to retrieve the wsdl from the url defined
 * 
 * @since 3.9
 */
public interface WsdlRetrieverStrategy 
{
	Definition retrieveWsdl() throws WSDLException;
}
