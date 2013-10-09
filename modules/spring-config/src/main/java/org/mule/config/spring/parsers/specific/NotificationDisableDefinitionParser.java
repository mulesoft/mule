/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.MuleDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildListEntryDefinitionParser;
import org.mule.config.spring.parsers.delegate.AttributeSelectionDefinitionParser;
import org.mule.config.spring.parsers.processors.CheckExclusiveAttributes;
import org.mule.config.spring.parsers.processors.CheckRequiredAttributes;

 public class NotificationDisableDefinitionParser extends AttributeSelectionDefinitionParser
{

    public static final String DISABLED_EVENT = "disabledType";
    public static final String DISABLED_INTERFACE = "disabledInterface";

    public NotificationDisableDefinitionParser()
    {
        super(NotificationDefinitionParser.EVENT,
                addMappingToConstructorArg(
                        new ChildListEntryDefinitionParser(DISABLED_EVENT,
                                NotificationDefinitionParser.EVENT)));

        addDelegate(NotificationDefinitionParser.EVENT_CLASS,
                new ChildListEntryDefinitionParser(DISABLED_EVENT,
                        NotificationDefinitionParser.EVENT_CLASS));

        MuleDefinitionParser parser =
                new ChildListEntryDefinitionParser(DISABLED_INTERFACE, NotificationDefinitionParser.INTERFACE);
        parser.addMapping(NotificationDefinitionParser.INTERFACE, NotificationDefinitionParser.INTERFACE_MAP);
        addDelegate(NotificationDefinitionParser.INTERFACE, parser);

        addDelegate(NotificationDefinitionParser.INTERFACE_CLASS,
                new ChildListEntryDefinitionParser(DISABLED_INTERFACE,
                        NotificationDefinitionParser.INTERFACE_CLASS));

        registerPreProcessor(new CheckExclusiveAttributes(NotificationDefinitionParser.ALL_ATTRIBUTES));
        registerPreProcessor(new CheckRequiredAttributes(NotificationDefinitionParser.ALL_ATTRIBUTES));
    }

    private static MuleDefinitionParser addMappingToConstructorArg(MuleDefinitionParser parser)
    {
        parser.addMapping(NotificationDefinitionParser.EVENT, NotificationDefinitionParser.EVENT_MAP);
        return parser;
    }

}
