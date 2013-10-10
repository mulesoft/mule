/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.servlet.jetty.functional;

import static org.junit.Assert.assertEquals;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.PatchMethod;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.junit.ClassRule;
import org.junit.Test;

public class JettyPatchMethodTestCase extends FunctionalTestCase
{
    @ClassRule
    public static DynamicPort port1 = new DynamicPort("port1");

    public JettyPatchMethodTestCase()
    {
        super();
        setDisposeContextPerClass(true);
    }

    @Override
    protected String getConfigResources()
    {
        return "jetty-patch-method.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        setupTestComponent();
    }

    private void setupTestComponent() throws Exception
    {
        FunctionalTestComponent ftc = (FunctionalTestComponent) getComponent("PatchWithCustomComponent");
        ftc.setEventCallback(new CheckMessageCallback());
    }

    @Test
    public void testPatch() throws Exception
    {
        String url = String.format("http://localhost:%d/component", port1.getNumber());
        PatchMethod method = new PatchMethod(url);
        int status = new HttpClient().executeMethod(method);
        assertEquals(HttpStatus.SC_OK, status);
    }

    private static class CheckMessageCallback implements EventCallback
    {
        @Override
        public void eventReceived(MuleEventContext context, Object component) throws Exception
        {
            MuleMessage message = context.getMessage();
            assertEquals(HttpConstants.METHOD_PATCH, message.getInboundProperty("http.method"));
        }
    }
}
