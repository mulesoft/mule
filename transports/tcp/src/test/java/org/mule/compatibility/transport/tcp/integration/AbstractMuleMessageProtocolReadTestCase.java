/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.tcp.integration;

import static org.junit.Assert.assertEquals;

import org.mule.compatibility.transport.tcp.TcpProtocol;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.transformer.wire.WireFormat;
import org.mule.runtime.core.transformer.wire.SerializedMuleMessageWireFormat;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import org.junit.Rule;
import org.junit.Test;

public abstract class AbstractMuleMessageProtocolReadTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort port = new DynamicPort("port");


    @Test
    public void testServer() throws Exception
    {
        MuleClient client = muleContext.getClient();
        safeProtocolSend("localhost", port.getNumber(), getTestMuleMessage(TEST_MESSAGE));
        MuleMessage response = client.request("vm://testOut", RECEIVE_TIMEOUT);
        assertEquals(TEST_MESSAGE, response.getPayload());
    }

    private void safeProtocolSend(String host, int port, MuleMessage msg) throws IOException, MuleException
    {
        Socket clientSocket = new Socket(host, port);
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        WireFormat wireFormat = new SerializedMuleMessageWireFormat();
        wireFormat.setMuleContext(muleContext);
        wireFormat.write(baos, msg, msg.getEncoding());
        TcpProtocol delegate = createMuleMessageProtocol();
        delegate.write(outToServer, baos.toByteArray());
        clientSocket.close();
    }

    protected abstract TcpProtocol createMuleMessageProtocol();

}
