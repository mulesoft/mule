/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transport.cxf;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.custommonkey.xmlunit.XMLAssert;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class CxfEnvelopePayloadWithWsdlTestCase extends FunctionalTestCase
{

    private static final String msg = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                       + "xmlns:emop=\"http://www.wcs.com/2010/07/14/emop\">"
                       + "  <soapenv:Header>\n"
                       + "    <header UserName=\"nothing\" Password=\"important\"/>\n"
                       + "  </soapenv:Header>\n"
                       + "  <soapenv:Body>\n"
                       + "    <emop:ScratchcardValidateAndPayRequestBody>\n"
                       + "       <ScratchcardNumber>1</ScratchcardNumber>\n"
                       + "       <VirnNumber>2</VirnNumber>\n"
                       + "    </emop:ScratchcardValidateAndPayRequestBody>\n"
                       + "  </soapenv:Body>\n"
                       + "</soapenv:Envelope>";

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/transport/cxf/scratchcard-service-config.xml";
    }

    @Test
    public void testEnvelopePayloadIsProcessedWhenMessageAndWsdlContainsHeaders() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        MuleMessage result = client.send("http://localhost:28182/ScratchCardServiceV1", msg, null);

        assertNotNull("The result shouln't have been null", result);
        final String payloadAsString = result.getPayloadAsString();
        XMLAssert.assertXMLEqual(msg, payloadAsString);
    }
}
