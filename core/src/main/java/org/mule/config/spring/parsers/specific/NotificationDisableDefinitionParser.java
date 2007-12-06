 /*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.collection.ChildListEntryDefinitionParser;
import org.mule.config.spring.parsers.processors.CheckExclusiveAttributes;
import org.mule.config.spring.parsers.processors.CheckRequiredAttributes;
import org.mule.config.spring.parsers.delegate.AttributeSelectionDefinitionParser;

 public class NotificationDisableDefinitionParser extends AttributeSelectionDefinitionParser
{

    public static final String DISABLED_EVENT = "disabledType";
    public static final String DISABLED_INTERFACE = "disabledInterface";

    public NotificationDisableDefinitionParser()
    {
        super(NotificationDefinitionParser.EVENT,
                new ChildListEntryDefinitionParser(DISABLED_EVENT,
                        NotificationDefinitionParser.EVENT)
                        .addMapping(NotificationDefinitionParser.EVENT,
                        NotificationDefinitionParser.EVENT_MAP));
        addDelegate(NotificationDefinitionParser.EVENT_CLASS,
                new ChildListEntryDefinitionParser(DISABLED_EVENT,
                        NotificationDefinitionParser.EVENT_CLASS));
        addDelegate(NotificationDefinitionParser.INTERFACE,
                new ChildListEntryDefinitionParser(DISABLED_INTERFACE,
                        NotificationDefinitionParser.INTERFACE)
                        .addMapping(NotificationDefinitionParser.INTERFACE,
                        NotificationDefinitionParser.INTERFACE_MAP));
        addDelegate(NotificationDefinitionParser.INTERFACE_CLASS,
                new ChildListEntryDefinitionParser(DISABLED_INTERFACE,
                        NotificationDefinitionParser.INTERFACE_CLASS));
        registerPreProcessor(new CheckExclusiveAttributes(NotificationDefinitionParser.ALL_ATTRIBUTES));
        registerPreProcessor(new CheckRequiredAttributes(NotificationDefinitionParser.ALL_ATTRIBUTES));
    }

}
