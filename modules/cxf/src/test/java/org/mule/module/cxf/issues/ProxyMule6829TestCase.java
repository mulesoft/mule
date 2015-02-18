/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.concurrent.Latch;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import javax.xml.namespace.QName;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ProxyMule6829TestCase extends FunctionalTestCase
{

    private static final HttpRequestOptions HTTP_REQUEST_OPTIONS = newOptions().method(POST.name()).disableStatusCodeValidation().build();

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Parameterized.Parameter
    public String configFile;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"proxy-mule-6829.xml"},
                {"proxy-mule-6829-httpn.xml"}
        });
    }

    @Override
    protected String getConfigFile()
    {
        return configFile;
    }

    private static class TestCxfEventCallback implements EventCallback
    {
        private Latch latch;
        private String cxfOperationName;

        private TestCxfEventCallback(Latch latch)
        {
            this.latch = latch;
        }

        @Override
        public void eventReceived(MuleEventContext context, Object component) throws Exception
        {
            QName cxfOperation = (QName)context.getMessage().getProperty("cxf_operation",
                PropertyScope.INVOCATION);
            cxfOperationName = cxfOperation.getLocalPart();

            latch.countDown();
        }

        public String getCxfOperationName()
        {
            return cxfOperationName;
        }
    }

    @Test
    public void testProxyServerSoap11() throws Exception
    {
         final Latch latch = new Latch();
         TestCxfEventCallback testCxfEventCallback = new TestCxfEventCallback(latch);
         FunctionalTestComponent testComponent = (FunctionalTestComponent) getComponent("soap11Flow");
         testComponent.setEventCallback(testCxfEventCallback);

         String msgEchoOperation1 = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:new=\"http://new.webservice.namespace\">"
                 + "<soapenv:Header/>"
                 + "  <soapenv:Body>"
                 + "    <new:parameter1>hello world</new:parameter1>"
                 + "  </soapenv:Body>"
                 + "</soapenv:Envelope>";

         String msgEchoOperation2 = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:new=\"http://new.webservice.namespace\">"
                 + "<soapenv:Header/>"
                 + "  <soapenv:Body>"
                 + "    <new:parameter2>hello world</new:parameter2>"
                 + "  </soapenv:Body>"
                 + "</soapenv:Envelope>";

         String soapOperation = "EchoOperation1";
         MuleMessage response = executeSoap11Call(msgEchoOperation1, soapOperation);
         assertTrue(latch.await(1000L, TimeUnit.MILLISECONDS));
         String cxfOperationName = testCxfEventCallback.getCxfOperationName();
         assertEquals(soapOperation, cxfOperationName);
         assertTrue(response.getPayloadAsString().contains("<new:parameter1"));
         assertTrue(response.getPayloadAsString().contains("hello world"));

         soapOperation = "EchoOperation2";
         response = executeSoap11Call(msgEchoOperation2, soapOperation);
         assertTrue(latch.await(1000L, TimeUnit.MILLISECONDS));
         cxfOperationName = testCxfEventCallback.getCxfOperationName();
         assertEquals(soapOperation, cxfOperationName);
         assertTrue(response.getPayloadAsString().contains("<new:parameter2"));
         assertTrue(response.getPayloadAsString().contains("hello world"));
    }

    @Test
    public void testProxyServerSoap12() throws Exception
    {
         final Latch latch = new Latch();
         TestCxfEventCallback testCxfEventCallback = new TestCxfEventCallback(latch);
         FunctionalTestComponent testComponent = (FunctionalTestComponent) getComponent("soap12Flow");
         testComponent.setEventCallback(testCxfEventCallback);

         String msgEchoOperation1 = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:new=\"http://new.webservice.namespace\">"
                 + "<soap:Header/>"
                 + "  <soap:Body>"
                 + "    <new:parameter1>hello world</new:parameter1>"
                 + "  </soap:Body>"
                 + "</soap:Envelope>";

         String msgEchoOperation2 = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:new=\"http://new.webservice.namespace\">"
                 + "<soap:Header/>"
                 + "  <soap:Body>"
                 + "    <new:parameter2>hello world</new:parameter2>"
                 + "  </soap:Body>"
                 + "</soap:Envelope>";

         String soapOperation = "EchoOperation1";
         MuleMessage response = executeSoap12Call(msgEchoOperation1, soapOperation);
         assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
         String cxfOperationName = testCxfEventCallback.getCxfOperationName();
         assertEquals(soapOperation, cxfOperationName);
         assertTrue(response.getPayloadAsString().contains("<new:parameter1"));
         assertTrue(response.getPayloadAsString().contains("hello world"));

         soapOperation = "EchoOperation2";
         response = executeSoap12Call(msgEchoOperation2, soapOperation);
         assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
         cxfOperationName = testCxfEventCallback.getCxfOperationName();
         assertEquals(soapOperation, cxfOperationName);
         assertTrue(response.getPayloadAsString().contains("<new:parameter2"));
         assertTrue(response.getPayloadAsString().contains("hello world"));
    }

    private MuleMessage executeSoap11Call(String msgString, String soapAction) throws MuleException
    {
        MuleMessage msg = getTestMuleMessage(msgString);
        msg.setProperty("soapAction", soapAction, PropertyScope.OUTBOUND);

        return muleContext.getClient().send("http://localhost:" + dynamicPort.getNumber() + "/EchoService11", msg, HTTP_REQUEST_OPTIONS);
    }

    private MuleMessage executeSoap12Call(String msgString, String soapAction) throws MuleException
    {
        MuleMessage msg = getTestMuleMessage(msgString);
        String contentType = "application/soap+xml;charset=UTF-8;action=\"" + soapAction + "\"";
        msg.setProperty("Content-Type", contentType, PropertyScope.OUTBOUND);

        return muleContext.getClient().send("http://localhost:" + dynamicPort.getNumber() + "/EchoService12", msg, HTTP_REQUEST_OPTIONS);
    }
}


