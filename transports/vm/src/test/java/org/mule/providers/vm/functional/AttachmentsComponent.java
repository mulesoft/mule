/*
 * $Id:AttachmentsComponent.java 7555 2007-07-18 03:17:16Z aperepel $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.vm.functional;

import org.mule.impl.MuleMessage;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.lifecycle.Callable;

import java.io.File;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

/**
 * A test component that reads inbound attachments and sends an attachment back. This class is only suitable
 * for the VMAttachementsTestCase.
 */
public class AttachmentsComponent implements Callable
{
        public Object onCall(UMOEventContext eventContext) throws Exception
        {
            UMOMessage msg = eventContext.getMessage();
            if (msg.getAttachmentNames().size() == 2)
            {
                throw new IllegalArgumentException("There shuold be 2 attachments");
            }


            DataHandler dh = msg.getAttachment("test-attachment");
            if (dh == null)
            {
                throw new IllegalArgumentException("test-attachment is not on the message");
            }
            if (!dh.getContentType().startsWith("text/xml"))
            {
                throw new IllegalArgumentException("content type is not text/xml");
            }

            if (!"Mmm... attachments!".equals(msg.getPayloadAsString()))
            {
                throw new IllegalArgumentException("payload is incorrect");
            }
            //Lets return an image
            UMOMessage result = new MuleMessage("here is one for you!");
            FileDataSource ds = new FileDataSource(new File("transports/vm/src/test/resources/test.gif").getAbsoluteFile());
            result.addAttachment("mule", new DataHandler(ds));
            return result;
        }
    }
