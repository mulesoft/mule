/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.module.client.MuleClient;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.concurrent.Latch;

import java.util.concurrent.TimeUnit;

import javax.xml.namespace.QName;

import org.junit.Rule;
import org.junit.Test;

public class ProxyMule6829TestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");
    
    /* (non-Javadoc)
     * @see org.mule.tck.junit4.FunctionalTestCase#getConfigResources()
     */
    @Override
    protected String getConfigResources()
    {
        return "proxy-mule-6829.xml";
    }
    
    private static class TestCxfEventCallback implements EventCallback {
        
        private Latch latch;
        private String cxfOperationName;
        
        private TestCxfEventCallback(Latch latch) {
            this.latch = latch;
        }
        
        public void eventReceived(MuleEventContext context, Object component) throws Exception
        {
            QName cxfOperation = (QName)context.getMessage().getProperty("cxf_operation", PropertyScope.INVOCATION);
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
    
    private MuleMessage executeSoap11Call(String msgString, String soapAction) throws MuleException {
        MuleMessage msg = new DefaultMuleMessage(msgString, muleContext);
        msg.setProperty("soapAction", soapAction, PropertyScope.OUTBOUND);

        MuleClient client = new MuleClient(muleContext);
        return client.send("http://localhost:" + dynamicPort.getNumber() + "/EchoService11", msg, null);
    }
    
    private MuleMessage executeSoap12Call(String msgString, String soapAction) throws MuleException {
        MuleMessage msg = new DefaultMuleMessage(msgString, muleContext);
        String contentType = "application/soap+xml;charset=UTF-8;action=\"" + soapAction + "\"";
        msg.setProperty("Content-Type", contentType, PropertyScope.OUTBOUND);

        MuleClient client = new MuleClient(muleContext);
        return client.send("http://localhost:" + dynamicPort.getNumber() + "/EchoService12", msg, null);
    }
}


