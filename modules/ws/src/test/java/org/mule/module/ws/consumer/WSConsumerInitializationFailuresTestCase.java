/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.consumer;

import static org.mule.tck.util.TestUtils.loadConfiguration;

import org.mule.api.config.ConfigurationException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;


import org.junit.Test;;


public class WSConsumerInitializationFailuresTestCase extends AbstractMuleContextTestCase
{

    @Test(expected = ConfigurationException.class)
    public void wsConsumerConfigUseConnectorToRetrieveWsdlNoHttp() throws Exception
    {
        loadConfiguration("ws-consumer-wsdl-no-connector-config-and-connector-to-retrieve.xml");
    }
    
    @Test(expected = ConfigurationException.class)
    public void wsConsumerConfigNullWsdlLocation() throws Exception
    {
        loadConfiguration("ws-consumer-wsdl-null-wsdl-location.xml");
    }


}
