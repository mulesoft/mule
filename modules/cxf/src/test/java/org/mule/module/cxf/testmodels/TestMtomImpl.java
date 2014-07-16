/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
