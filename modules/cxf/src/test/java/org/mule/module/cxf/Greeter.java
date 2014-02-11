/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.ws.BindingType;

@WebService
@BindingType(javax.xml.ws.soap.SOAPBinding.SOAP11HTTP_BINDING)
public interface Greeter
{
    @WebResult(name="name")
    public String greet(@WebParam(name = "name") String name);
}



