/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.employee;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import static org.mule.transport.http.HttpConnector.HTTP_STATUS_PROPERTY;
import static org.mule.transport.http.HttpConstants.SC_OK;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Prober;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class MtomClientTestCase extends AbstractServiceAndFlowTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public MtomClientTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                                             {ConfigVariant.SERVICE, "mtom-client-conf-service.xml"},
                                             {ConfigVariant.FLOW, "mtom-client-conf-flow.xml"},
                                             {ConfigVariant.FLOW, "mtom-client-conf-flow-httpn.xml"}
        });
    }

    @Test
    public void testEchoService() throws Exception
    {
        final EmployeeDirectoryImpl svc = (EmployeeDirectoryImpl) getComponent("employeeDirectoryService");

        Prober prober = new PollingProber(6000, 500);
        prober.check(new JUnitProbe()
        {
            @Override
            protected boolean test() throws Exception
            {
                assertThat(svc.getInvocationCount(), is(greaterThanOrEqualTo(1)));
                return true;
            }

            @Override
            public String describeFailure()
            {
                return "Expected invocation count to be at least 1.";
            }
        });

        // ensure that an attachment was actually sent.
        assertTrue(AttachmentVerifyInterceptor.HasAttachments);
    }


    /**
     * According to <a href=
     * "https://issues.apache.org/jira/browse/CXF-6665?focusedCommentId=14991991&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-14991991">https://issues.apache.org/jira/browse/CXF-6665?focusedCommentId=14991991&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-14991991</a>
     * 
     * @throws MuleException
     * @throws IOException
     */
    @Test
    public void nonConformantSoap12MultipartRequestHeader() throws MuleException, IOException
    {
        // Same as the header sent by the flow in 'testEchoService', but 'start-info' is removed and type changed from
        // 'application/xop+xml' to 'application/soap+xml'
        String nonConformantContentTypeHeader =
                "multipart/related; type=\"application/soap+xml\"; boundary=\"uuid:c8f318eb-abd2-4c6c-ac42-1cf785e578a9\"; start=\"<root.message@cxf.apache.org>\"; charset=UTF-8";

        HttpRequestOptions HTTP_REQUEST_OPTIONS = newOptions().method(POST.name()).disableStatusCodeValidation().build();

        MuleClient client = muleContext.getClient();
        MuleMessage message = getTestMuleMessage("--uuid:c8f318eb-abd2-4c6c-ac42-1cf785e578a9\n" +
                                                 // Replaced this content type from 'application/xop+xml; charset=UTF-8; type="text/xml"'
                                                 "Content-Type: application/soap+xml; charset=UTF-8\n" +
                                                 "Content-Transfer-Encoding: binary\n" +
                                                 "Content-ID: <root.message@cxf.apache.org>\n" +
                                                 "\n" +
                                                 "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\"><soap:Body><addEmployee xmlns=\"http://employee.example.mule.org/\"><employee><division>Theoretical Physics</division><name>Albert Einstein</name><picture><xop:Include xmlns:xop=\"http://www.w3.org/2004/08/xop/include\" href=\"cid:40d88be5-db4c-457b-84d4-a06a83965f34-1@employee.example.mule.org\"/></picture></employee></addEmployee></soap:Body></soap:Envelope>\n"
                                                 +
                                                 "--uuid:c8f318eb-abd2-4c6c-ac42-1cf785e578a9\n" +
                                                 "Content-Type: image/jpeg\n" +
                                                 "Content-Transfer-Encoding: binary\n" +
                                                 "Content-ID: <40d88be5-db4c-457b-84d4-a06a83965f34-1@employee.example.mule.org>\n" +
                                                 "Content-Disposition: attachment;name=\"albert_einstein.jpg\"\n" +
                                                 "\n" +
                                                 "aaa\n" +
                                                 "--uuid:c8f318eb-abd2-4c6c-ac42-1cf785e578a9--");

        message.setOutboundProperty("SOAPAction", "");
        message.setOutboundProperty("Content-Type", nonConformantContentTypeHeader);

        MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/employeeDirectory12", message, HTTP_REQUEST_OPTIONS);

        assertThat((Integer) result.getInboundProperty(HTTP_STATUS_PROPERTY), is(SC_OK));
    }
}
