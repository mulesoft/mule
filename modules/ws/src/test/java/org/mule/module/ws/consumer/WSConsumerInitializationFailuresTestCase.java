/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.consumer;

import static org.mule.api.config.MuleProperties.MULE_USE_CONNECTOR_TO_RETRIEVE_WSDL;
import static org.mule.tck.util.TestUtils.loadConfiguration;

import org.mule.api.config.ConfigurationException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;;


public class WSConsumerInitializationFailuresTestCase extends AbstractMuleContextTestCase
{

    @Rule
    public SystemProperty useConnectorToRetrieveWsdl = new SystemProperty(MULE_USE_CONNECTOR_TO_RETRIEVE_WSDL, "true");

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void wsConsumerConfigNullWsdlLocation() throws Exception
    {
        exception.expect(ConfigurationException.class);
        exception.expectMessage("wsdlLocation");;
        loadConfiguration("ws-consumer-wsdl-null-wsdl-location.xml");
    }


}
