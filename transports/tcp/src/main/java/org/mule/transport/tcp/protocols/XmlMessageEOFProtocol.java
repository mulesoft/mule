/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp.protocols;

/**
 * Extend {@link org.mule.transport.tcp.protocols.XmlMessageProtocol} to continue reading
 * until either a new message or EOF is found.
 */
public class XmlMessageEOFProtocol extends XmlMessageProtocol
{

    /**
     * Continue reading til EOF or new document found
     *
     * @param patternIndex The index of the xml tag (or -1 if the next message not found)
     * @param len The amount of data read this loop (or -1 if EOF)
     * @param available The amount of data available to read
     * @return true if the read should continue
     */
    @Override
    protected boolean isRepeat(int patternIndex, int len, int available)
    {
        return patternIndex < 0;
    }

}
