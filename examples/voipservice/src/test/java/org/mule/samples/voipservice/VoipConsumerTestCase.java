/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.voipservice;

import org.mule.samples.voipservice.client.VoipConsumer;

import junit.framework.TestCase;

public class VoipConsumerTestCase extends TestCase
{

    // this is just a direct call to the example.
    // if we call this inside a typical FunctionalTestCase the ports
    // are already bound by the test case's own mule instance
    public void testConsumerMain() throws Exception
    {
        VoipConsumer voipConsumer = new VoipConsumer("voip-broker-sync-config.xml");
        voipConsumer.requestSend("vm://VoipBrokerRequests");
    }

}
