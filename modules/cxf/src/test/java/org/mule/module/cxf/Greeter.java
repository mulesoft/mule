/*
 * $Id: Greeter.java 18408 2010-12-09 19:44:58Z travis.carlson $
 * --------------------------------------------------------------------------------------
 *
 * (c) 2003-2010 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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



