/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp.config;

import org.mule.config.spring.parsers.delegate.BooleanAttributeSelectionDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;

public class ByteOrMessageProtocolDefinitionParser extends BooleanAttributeSelectionDefinitionParser
{

    public static final String PROTOCOL = "tcpProtocol";

    public ByteOrMessageProtocolDefinitionParser(Class bytes, Class message)
    {
        super("payloadOnly", true,
                new ChildDefinitionParser(PROTOCOL, bytes),
                new ChildDefinitionParser(PROTOCOL, message));
    }

}
