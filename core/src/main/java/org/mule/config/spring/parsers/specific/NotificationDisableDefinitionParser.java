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

public class NotificationDisableDefinitionParser extends ChildListEntryDefinitionParser
{

    public NotificationDisableDefinitionParser()
    {
        super("disableInterface", NotificationEnableDefinitionParser.INTERFACE_CLASS);
        addMapping(NotificationEnableDefinitionParser.INTERFACE, NotificationEnableDefinitionParser.INTERFACE_MAP);
        addAlias(NotificationEnableDefinitionParser.INTERFACE, VALUE);
        removeIgnored(NotificationEnableDefinitionParser.INTERFACE);
        registerPreProcessor(new CheckExclusiveAttributes(NotificationEnableDefinitionParser.INTERFACE_ATTRIBUTES));
        registerPreProcessor(new CheckRequiredAttributes(NotificationEnableDefinitionParser.INTERFACE_ATTRIBUTES));
    }

}
