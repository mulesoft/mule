/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp.integration;

import static org.junit.Assert.assertEquals;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.transformer.wire.WireFormat;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transformer.wire.SerializedMuleMessageWireFormat;
import org.mule.transport.tcp.TcpProtocol;

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
        LocalMuleClient client = muleContext.getClient();
        safeProtocolSend("localhost", port.getNumber(), new DefaultMuleMessage(TEST_MESSAGE, muleContext));
        MuleMessage response = client.request("vm://testOut", RECEIVE_TIMEOUT);
        assertEquals(TEST_MESSAGE, response.getPayload());
    }

    private void safeProtocolSend(String host, int port, DefaultMuleMessage msg) throws IOException, MuleException
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
