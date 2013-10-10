/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.testmodels;

import java.io.IOException;
import java.io.InputStream;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.jws.WebService;
import javax.xml.ws.Holder;

import org.apache.cxf.mime.TestMtom;
import org.apache.cxf.mime.types.XopStringType;

@WebService(serviceName = "TestMtomService", portName = "TestMtomPort", targetNamespace = "http://cxf.apache.org/mime", endpointInterface = "org.apache.cxf.mime.TestMtom", wsdlLocation = "testutils/mtom_xop.wsdl")
public class TestMtomImpl implements TestMtom
{

    public XopStringType testXopString(XopStringType data)
    {
        return data;
    }

    public void testXop(Holder<String> name, Holder<DataHandler> attachinfo)
    {
        name.value = "return detail + " + name.value;

        try
        {
            InputStream inputStream = attachinfo.value.getInputStream();
            while (inputStream.read() != -1);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        attachinfo.value = new DataHandler(new FileDataSource("src/test/resources/mtom-conf-service.xml"));
    }

}
