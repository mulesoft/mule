/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.consumer;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Map;

import org.junit.Test;

/**
 * Retrieves a wsdl from an archive which has an imported schema
 * which has a relative location within the jar
 */
@SmallTest
public class WSConsumerArchivedWSDLTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {

        return "ws-consumer-archived-wsdl.xml";
    }

    @Test
    public void consumerPresentInRegistry() throws Exception
    {
        Map<String, WSConsumer> consumers = muleContext.getRegistry().lookupByType(WSConsumer.class);
        // if one consumer is present in the registry, the config was correctly initialized
        assertThat(consumers.values().size(), equalTo(1));
    }

}
