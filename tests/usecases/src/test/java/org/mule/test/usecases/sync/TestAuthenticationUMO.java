/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.usecases.sync;

import org.mule.umo.lifecycle.Callable;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOMessage;
import org.mule.MuleException;

/**
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class TestAuthenticationUMO implements Callable
{
    public Object onCall(UMOEventContext eventContext) throws Exception
    {
        String payload  = eventContext.getTransformedMessageAsString();
        if(!payload.equals("ross")) {
            //Calling this tells mule not to go any further and return
            eventContext.setStopFurtherProcessing(true);
            return "User not Authorised";
        }

        //When the Sync replyTo bug is fixed, you'll just need to return the message
        //return "User " + payload + " Authorised";

        //dispatch the message ourself. Passing use the message in causes Mule
        //to dispatch the event via the configured outbound router
        eventContext.dispatchEvent("User " + payload + " Authorised");
        UMOMessage message = eventContext.receiveEvent("jms://ResultQueue", 5000);
        //Calling this tells mule not to go any further and return
        eventContext.setStopFurtherProcessing(true);
        return "Received: " + message.getPayloadAsString();
    }
}
