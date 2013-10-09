/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertTrue;

public class ProxySoapVersionTestCase extends AbstractServiceAndFlowTestCase
{
    String doGoogleSearch = "<urn:doGoogleSearch xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:urn=\"urn:GoogleSearch\">";

    // Message using Soap 1.2 version
    String msgWithComment = "<soap12:Envelope xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\">"
        + "<!-- comment 1 -->"
        + "<soap12:Header>"
        + "<!-- comment 2 -->"
        + "</soap12:Header>"
        + "<!-- comment 3 -->"
        + "<soap12:Body>"
        + "<!-- comment 4 -->"
        + doGoogleSearch
        + "<!-- this comment breaks it -->"
        + "<key>1</key>"
        + "<!-- comment 5 -->"
        + "<q>a</q>"
        + "<start>0</start>"
        + "<maxResults>1</maxResults>"
        + "<filter>false</filter>"
        + "<restrict>a</restrict>"
        + "<safeSearch>true</safeSearch>"
        + "<lr>a</lr>"
        + "<ie>b</ie>"
        + "<oe>c</oe>"
        + "</urn:doGoogleSearch>"
        + "<!-- comment 6 -->"
        + "</soap12:Body>"
        + "<!-- comment 7 -->"
        + "</soap12:Envelope>";

    public ProxySoapVersionTestCase(ConfigVariant variant, String configResources)
    {
        super(variant,  configResources);
    }

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {AbstractServiceAndFlowTestCase.ConfigVariant.SERVICE, "proxy-soap-version-conf-service.xml"},
                {AbstractServiceAndFlowTestCase.ConfigVariant.FLOW, "proxy-soap-version-conf-flow.xml"}
        });
    }

    @Test
    public void testProxyWithCommentInRequest() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/proxy-soap-version", msgWithComment, null);
        String resString = result.getPayloadAsString();
        assertTrue(resString.contains(doGoogleSearch));
    }
}
