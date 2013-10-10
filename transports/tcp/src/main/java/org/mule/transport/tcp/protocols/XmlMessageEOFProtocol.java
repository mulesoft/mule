/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
