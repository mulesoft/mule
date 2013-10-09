/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.Holder;

@WebService
public interface HolderService
{
    String echo(String s1, @WebParam(mode= WebParam.Mode.OUT)
                Holder<String> outS1,@WebParam(mode= WebParam.Mode.OUT)
                Holder<String> outS2);

    String echo2(String s1, @WebParam(mode = WebParam.Mode.OUT)
                 Holder<String> outS1, String s2);

    void echo3(@WebParam(mode = WebParam.Mode.OUT)
                 Holder<String> outS1, String s1);

}
