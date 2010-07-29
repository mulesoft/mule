/*
 * $Id: MessageProcessorDefinitionParser.java 17725 2010-06-25 20:12:28Z tcarlson $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific;

import org.mule.api.source.MessageSource;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;

public class MessageSourceDefinitionParser extends ChildDefinitionParser
{
    public static final String SOURCE = "messageSource";

    public MessageSourceDefinitionParser(Class clazz)
    {
        super(SOURCE, clazz);
    }

    public MessageSourceDefinitionParser()
    {
        super(SOURCE, null, MessageSource.class);
    }

}
