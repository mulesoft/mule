/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
    protected String getConfigFile()
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
