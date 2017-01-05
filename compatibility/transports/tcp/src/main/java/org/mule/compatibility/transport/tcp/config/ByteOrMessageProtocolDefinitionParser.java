/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.tcp.config;

import org.mule.runtime.config.spring.parsers.delegate.BooleanAttributeSelectionDefinitionParser;
import org.mule.runtime.config.spring.parsers.generic.ChildDefinitionParser;

public class ByteOrMessageProtocolDefinitionParser extends BooleanAttributeSelectionDefinitionParser {

  public static final String PROTOCOL = "tcpProtocol";

  public ByteOrMessageProtocolDefinitionParser(Class bytes, Class message) {
    super("payloadOnly", true, new ChildDefinitionParser(PROTOCOL, bytes), new ChildDefinitionParser(PROTOCOL, message));
  }

}
