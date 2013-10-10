/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf;

import javax.jws.WebService;
import javax.xml.ws.Holder;

@WebService(endpointInterface = "org.mule.module.cxf.HolderService")
public class HolderServiceImpl implements HolderService
{
    public String echo(String s1, Holder<String> outS1, Holder<String> outS2)
    {
        outS1.value = s1 + "-holder1";
        outS2.value = s1 + "-holder2";
        return s1 + "-response";
    }

    public String echo2(String s1, Holder<String> outS1, String s2)
    {
        outS1.value = s2 + "-holder";
        return s1 + "-response";
    }

    public void echo3(Holder<String> outS1, String s1)
    {
        outS1.value = s1;
    }


}
