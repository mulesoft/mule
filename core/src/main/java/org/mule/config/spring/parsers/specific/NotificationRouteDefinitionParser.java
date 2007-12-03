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

import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class NotificationRouteDefinitionParser extends ChildMapEntryDefinitionParser
{

    public static final Map EVENT_MAP;

    static
    {
        EVENT_MAP = new HashMap();
        EVENT_MAP.put("MANAGER", org.mule.impl.internal.notifications.ManagerNotification.class.getName());
        EVENT_MAP.put("MODEL", org.mule.impl.internal.notifications.ModelNotification.class.getName());
        EVENT_MAP.put("COMPONENT", org.mule.impl.internal.notifications.ComponentNotification.class.getName());
        EVENT_MAP.put("SECURITY", org.mule.impl.internal.notifications.SecurityNotification.class.getName());
        EVENT_MAP.put("MANAGEMENT", org.mule.impl.internal.notifications.ManagementNotification.class.getName());
        EVENT_MAP.put("ADMIN", org.mule.impl.internal.notifications.AdminNotification.class.getName());
        EVENT_MAP.put("CONNECTION", org.mule.impl.internal.notifications.ConnectionNotification.class.getName());
        EVENT_MAP.put("REGISTRY", org.mule.impl.internal.notifications.RegistryNotification.class.getName());
        EVENT_MAP.put("CUSTOM", org.mule.impl.internal.notifications.CustomNotification.class.getName());
        EVENT_MAP.put("MESSAGE", org.mule.impl.internal.notifications.MessageNotification.class.getName());
        EVENT_MAP.put("EXCEPTION", org.mule.impl.internal.notifications.ExceptionNotification.class.getName());
        EVENT_MAP.put("TRANSACTION", org.mule.impl.internal.notifications.TransactionNotification.class.getName());
    }

    public NotificationRouteDefinitionParser()
    {
        super("appendEventType", "interface", "event-class");
        addMapping("event", EVENT_MAP);
        addAlias("event", "value");
    }

    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        super.parseChild(element, parserContext, builder);    //To change body of overridden methods use File | Settings | File Templates.
    }

}
