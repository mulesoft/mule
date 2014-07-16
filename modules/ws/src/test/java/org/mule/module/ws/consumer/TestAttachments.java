/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.consumer;

import org.mule.util.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.ws.Holder;
import javax.xml.ws.soap.MTOM;

@MTOM
@WebService(portName = "TestAttachmentsPort", serviceName = "TestAttachmentsService")
public class TestAttachments
{

    @WebResult(name = "result")
    @WebMethod(action = "uploadAttachment")
    public String uploadAttachment(@WebParam(mode = WebParam.Mode.IN, name = "fileName") String fileName,
                                   @WebParam(mode = WebParam.Mode.IN, name = "attachment") DataHandler attachment)
    {
        try
        {
            InputStream received = attachment.getInputStream();
            InputStream expected = IOUtils.getResourceAsStream(fileName, getClass());

            if (IOUtils.contentEquals(received, expected))
            {
                return "OK";
            }
            else
            {
                return "UNEXPECTED CONTENT";
            }
        }
        catch (IOException e)
        {
            return "ERROR " + e.getMessage();
        }
    }

    @WebResult(name = "attachment")
    @WebMethod(action = "downloadAttachment")
    public DataHandler downloadAttachment(@WebParam(mode = WebParam.Mode.IN, name = "fileName") String fileName)
    {
        File file = new File(IOUtils.getResourceAsUrl(fileName, getClass()).getPath());
        return new DataHandler(new FileDataSource(file));
    }

    @WebMethod(action = "echoAttachment")
    public void echoAttachment(@WebParam(mode = WebParam.Mode.INOUT, name = "attachment") Holder<DataHandler> attachment)
    {
        // Do nothing.
    }

}
