/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.builder;

import org.mule.api.MuleException;
import org.mule.module.cxf.CxfInboundMessageProcessor;
import org.mule.module.cxf.testmodels.Echo;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import javax.xml.namespace.QName;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class WebServiceMessageProcessorBuilderTestCase extends AbstractMuleContextTestCase
{
    private WebServiceMessageProcessorBuilder serviceMessageProcessorBuilder;
    private static final String SERVICE_NAME = "Echo";
    private static final String NAMESPACE = "http://cxf.apache.org/";

    @Before
    public void setUp()
    {
        serviceMessageProcessorBuilder = new WebServiceMessageProcessorBuilder();
    }

    @Test
    public void testBuildServiceAttribute() throws MuleException
    {
        serviceMessageProcessorBuilder.setService(SERVICE_NAME);
        serviceMessageProcessorBuilder.setNamespace(NAMESPACE);
        serviceMessageProcessorBuilder.setMuleContext(muleContext);
        serviceMessageProcessorBuilder.setServiceClass(Echo.class);

        CxfInboundMessageProcessor messageProcessor = serviceMessageProcessorBuilder.build();
        assertNotNull(messageProcessor);
        QName serviceName = messageProcessor.getServer().getEndpoint().getService().getName();
        assertEquals(new QName(NAMESPACE, SERVICE_NAME), serviceName);
    }




}
