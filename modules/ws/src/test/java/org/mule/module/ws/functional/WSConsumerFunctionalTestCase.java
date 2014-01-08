/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.transport.NullPayload;
import org.mule.util.ClassUtils;

import java.util.Arrays;
import java.util.Collection;

import org.apache.cxf.interceptor.Fault;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class WSConsumerFunctionalTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port");

    @Rule
    public SystemProperty baseDir = new SystemProperty("baseDir", ClassUtils.getClassPathRoot(getClass()).getPath());

    private final String configFile;

    public WSConsumerFunctionalTestCase(String configFile)
    {
        this.configFile = configFile;
    }

    @Override
    protected String getConfigFile()
    {
        return configFile;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[] {"ws-consumer-http-config.xml"}, new Object[] {"ws-consumer-https-config.xml"}, new Object[] {"ws-consumer-jms-config.xml"});
    }

    @Test
    public void validRequestReturnsExpectedAnswer() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://in", "<tns:echo xmlns:tns=\"http://consumer.ws.module.mule.org/\"><text>Hello</text></tns:echo>", null);
        assertTrue(response.getPayloadAsString().contains("<text>Hello</text>"));
    }

    @Test
    public void invalidRequestFormatReturnsSOAPFault() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://in", "<tns:echo xmlns:tns=\"http://consumer.ws.module.mule.org/\"><invalid>Hello</invalid></tns:echo>", null);
        assertEquals(NullPayload.getInstance(), response.getPayload());
        assertTrue(response.getExceptionPayload().getException().getCause() instanceof Fault);
    }

    @Test
    public void invalidNamespaceReturnsSOAPFault() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://in", "<tns:echo xmlns:tns=\"http://invalid/\"><text>Hello</text></tns:echo>", null);
        assertEquals(NullPayload.getInstance(), response.getPayload());
        assertTrue(response.getExceptionPayload().getException().getCause() instanceof Fault);
    }

}
