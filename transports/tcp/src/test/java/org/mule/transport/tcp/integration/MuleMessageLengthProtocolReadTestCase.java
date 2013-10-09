/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp.integration;

import org.mule.transport.tcp.TcpProtocol;
import org.mule.transport.tcp.protocols.LengthProtocol;

public class MuleMessageLengthProtocolReadTestCase extends AbstractMuleMessageProtocolReadTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "mule-message-length-protocol-read-config.xml";
    }

    @Override
    protected TcpProtocol createMuleMessageProtocol()
    {
        return new LengthProtocol();
    }

}
