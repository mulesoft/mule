/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.voipservice;

import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.samples.voipservice.to.CreditCardTO;
import org.mule.samples.voipservice.to.CreditProfileTO;
import org.mule.samples.voipservice.to.CustomerTO;
import org.mule.samples.voipservice.to.ServiceParamTO;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public class VoipConsumerTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "voip-broker-sync-config.xml";
    }

    public void testRequestSend() throws Exception
    {
        UMOMessage result;
        CustomerTO customerTO = CustomerTO.getRandomCustomer();
        CreditCardTO creditCardTO = CreditCardTO.getRandomCreditCard();
        MuleClient client = new MuleClient();
        result = client.send("VoipBrokerRequests", new ServiceParamTO(customerTO, creditCardTO), null);
        CreditProfileTO creditProfileTO = (CreditProfileTO)((MuleMessage)result).getPayload();
        boolean valid = creditProfileTO.isValid();
    }

}
