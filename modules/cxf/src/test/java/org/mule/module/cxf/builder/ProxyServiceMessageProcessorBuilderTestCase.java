/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.builder;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleException;
import org.mule.module.cxf.CxfInboundMessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;


public class ProxyServiceMessageProcessorBuilderTestCase extends AbstractMuleContextTestCase
{

    private static final String SERVICE_NAME = "SLI";
    private static final String NAMESPACE = "http://SalesLead/CMS";

    @Test
    public void testBuildServiceAttribute() throws MuleException
    {
        ProxyServiceMessageProcessorBuilder serviceMessageProcessorBuilder = new ProxyServiceMessageProcessorBuilder();
        serviceMessageProcessorBuilder.setService(SERVICE_NAME);
        serviceMessageProcessorBuilder.setNamespace(NAMESPACE);
        serviceMessageProcessorBuilder.setMuleContext(muleContext);
        serviceMessageProcessorBuilder.setWsdlLocation("service/SLI.wsdl");

        CxfInboundMessageProcessor messageProcessor = serviceMessageProcessorBuilder.build();

        assertThat(messageProcessor.getServer().getEndpoint().getService().getServiceInfos().get(0).getSchemas().get(0).getSchema().getTargetNamespace(), is(notNullValue()));
    }

}
