/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp.config;

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
