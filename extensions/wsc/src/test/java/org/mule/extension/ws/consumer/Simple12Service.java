/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.consumer;

import javax.jws.WebService;
import javax.xml.ws.BindingType;
import javax.xml.ws.soap.MTOM;
import javax.xml.ws.soap.SOAPBinding;

/**
 * Web service11 using SOAP 1.2 used by WS Consumer tests.
 *
 * @since 4.0
 */
@MTOM
@WebService(portName = "TestPort", serviceName = "TestService")
@BindingType(value = SOAPBinding.SOAP12HTTP_BINDING)
public class Simple12Service extends Simple11Service {

}
