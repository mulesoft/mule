/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.Holder;

@WebService
public interface HolderService
{
    String echo(String s1,
                @WebParam(mode= WebParam.Mode.OUT) Holder<String> outS1,
                @WebParam(mode= WebParam.Mode.OUT) Holder<String> outS2);

    String echo2(String s1,
                 @WebParam(mode = WebParam.Mode.OUT) Holder<String> outS1,
                 String s2);

    void echo3(@WebParam(mode = WebParam.Mode.OUT) Holder<String> outS1,
               String s1);

}
