/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis.functional;

import org.mule.tck.functional.FunctionalTestComponent;

import javax.activation.DataHandler;

public class SoapAttachmentsFunctionalTestComponent extends FunctionalTestComponent
    implements SoapAttachmentsFunctionalTest
{
    public String receiveMessageWithAttachments(String payload, DataHandler[] attachments)
    {
        if (payload != null && attachments != null && attachments.length > 0)
        {
            return "Done";
        }

        return null;
    }
}
