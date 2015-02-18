/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transport.cxf;

import static org.junit.Assert.assertNotNull;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.transport.http.HttpConstants;

import java.util.Arrays;
import java.util.Collection;

import org.custommonkey.xmlunit.XMLAssert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class CxfEnvelopePayloadWithWsdlTestCase extends AbstractServiceAndFlowTestCase
{
    private static final String msg = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                       + "xmlns:emop=\"http://www.wcs.com/2010/07/14/emop\">" + "  <soapenv:Header>\n"
                       + "    <header UserName=\"nothing\" Password=\"important\"/>\n"
                       + "  </soapenv:Header>\n" + "  <soapenv:Body>\n"
                       + "    <emop:ScratchcardValidateAndPayRequestBody>\n"
                       + "       <ScratchcardNumber>1</ScratchcardNumber>\n"
                       + "       <VirnNumber>2</VirnNumber>\n"
                       + "    </emop:ScratchcardValidateAndPayRequestBody>\n" + "  </soapenv:Body>\n"
                       + "</soapenv:Envelope>";

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE,
                "org/mule/test/integration/transport/cxf/scratchcard-service-config-service.xml"},
            {ConfigVariant.FLOW,
                "org/mule/test/integration/transport/cxf/scratchcard-service-config-flow.xml"}});
    }

    public CxfEnvelopePayloadWithWsdlTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testEnvelopePayloadIsProcessedWhenMessageAndWsdlContainsHeaders() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("http://localhost:28182/ScratchCardServiceV1", getTestMuleMessage(msg), newOptions().disableStatusCodeValidation().method(HttpConstants.METHOD_POST).build());
        assertNotNull("The result shouln't have been null", result);

        final String payloadAsString = result.getPayloadAsString();
        XMLAssert.assertXMLEqual(msg, payloadAsString);
    }
}
