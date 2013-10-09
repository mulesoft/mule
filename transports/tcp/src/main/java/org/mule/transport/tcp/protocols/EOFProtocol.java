/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp.protocols;

/**
 * The EOFProtocol class is an application level tcp protocol that does nothing.
 * Reading is terminated by the stream being closed by the client.
 */
public class EOFProtocol extends DirectProtocol
{

    /**
     * Repeat until end of file
     *
     * @param len Amount transferred last call (-1 on EOF or socket error)
     * @param available Amount available
     * @return true if the transfer should continue
     */
    @Override
    protected boolean isRepeat(int len, int available)
    {
        return true;
    }

}
