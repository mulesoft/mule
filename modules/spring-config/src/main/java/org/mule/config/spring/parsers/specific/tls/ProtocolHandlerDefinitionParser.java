/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific.tls;

import org.mule.config.spring.parsers.generic.ParentDefinitionParser;

/**
 * @deprecated This was necessary pre Java SE 5.0. It will be ignored and removed in Mule 4
 */
@Deprecated
public class ProtocolHandlerDefinitionParser extends ParentDefinitionParser
{

    public ProtocolHandlerDefinitionParser()
    {
        // Property deprecated and ignored since 3.5
        addIgnored("property");
    }

}
