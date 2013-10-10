/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.transport.cxf;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.custommonkey.xmlunit.XMLAssert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertNotNull;

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
        MuleClient client = new MuleClient(muleContext);

        MuleMessage result = client.send("http://localhost:28182/ScratchCardServiceV1", msg, null);

        assertNotNull("The result shouln't have been null", result);
        final String payloadAsString = result.getPayloadAsString();
        XMLAssert.assertXMLEqual(msg, payloadAsString);
    }
}
