/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp.integration;

import org.mule.transport.tcp.TcpProtocol;
import org.mule.transport.tcp.protocols.EOFProtocol;

public class MuleMessageEofProtocolReadTestCase extends AbstractMuleMessageProtocolReadTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "mule-message-eof-protocol-read-config.xml";
    }

    @Override
    protected TcpProtocol createMuleMessageProtocol()
    {
        return new EOFProtocol();
    }
}
