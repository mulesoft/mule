/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.usecases.properties;

import org.mule.RequestContext;
import org.mule.routing.outbound.StaticRecipientList;

import java.util.ArrayList;
import java.util.List;

public class DummyComponent
{

    public void processData(String theData)
    {
        List recipients = new ArrayList();
        recipients.add("ross.mason@symphonysoft.com");
        recipients.add("ross@rossmason.com");
        RequestContext.getEventContext().getMessage().setOutboundProperty(StaticRecipientList.RECIPIENTS_PROPERTY,
                                                                          recipients);
    }

}
