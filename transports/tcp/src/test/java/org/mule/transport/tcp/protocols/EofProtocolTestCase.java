/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp.protocols;

import org.mule.transport.tcp.protocols.EOFProtocol;

public class EofProtocolTestCase extends DefaultProtocolTestCase
{

    public EofProtocolTestCase()
    {
        super(new EOFProtocol(), SlowInputStream.FULL_LENGTH);
    }

}
