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
import org.mule.config.spring.parsers.processors.CheckExclusiveAttributes;
import org.mule.config.spring.parsers.processors.CheckRequiredAttributes;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class NotificationEnableDefinitionParser extends ChildMapEntryDefinitionParser
{

    public static final Map EVENT_MAP;
    public static final Map INTERFACE_MAP;
    public static final String INTERFACE = "interface";
    public static final String INTERFACE_CLASS = "interface-class";
    public static final String EVENT = "event";
    public static final String EVENT_CLASS = "event-class";
    public static final String[][] INTERFACE_ATTRIBUTES =
            new String[][]{new String[]{INTERFACE}, new String[]{INTERFACE_CLASS}};
    public static final String[][] EVENT_ATTRIBUTES =
            new String[][]{new String[]{EVENT}, new String[]{EVENT_CLASS}};

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

        INTERFACE_MAP = new HashMap();
        INTERFACE_MAP.put("MANAGER", org.mule.impl.internal.notifications.ManagerNotificationListener.class.getName());
        INTERFACE_MAP.put("MODEL", org.mule.impl.internal.notifications.ModelNotificationListener.class.getName());
        INTERFACE_MAP.put("COMPONENT", org.mule.impl.internal.notifications.ComponentNotificationListener.class.getName());
        INTERFACE_MAP.put("SECURITY", org.mule.impl.internal.notifications.SecurityNotificationListener.class.getName());
        INTERFACE_MAP.put("MANAGEMENT", org.mule.impl.internal.notifications.ManagementNotificationListener.class.getName());
        INTERFACE_MAP.put("ADMIN", org.mule.impl.internal.notifications.AdminNotificationListener.class.getName());
        INTERFACE_MAP.put("CONNECTION", org.mule.impl.internal.notifications.ConnectionNotificationListener.class.getName());
        INTERFACE_MAP.put("REGISTRY", org.mule.impl.internal.notifications.RegistryNotificationListener.class.getName());
        INTERFACE_MAP.put("CUSTOM", org.mule.impl.internal.notifications.CustomNotificationListener.class.getName());
        INTERFACE_MAP.put("MESSAGE", org.mule.impl.internal.notifications.MessageNotificationListener.class.getName());
        INTERFACE_MAP.put("EXCEPTION", org.mule.impl.internal.notifications.ExceptionNotificationListener.class.getName());
        INTERFACE_MAP.put("TRANSACTION", org.mule.impl.internal.notifications.TransactionNotificationListener.class.getName());
    }

    public NotificationEnableDefinitionParser()
    {
        super("appendEventType", INTERFACE_CLASS, EVENT_CLASS);
        addMapping(EVENT, EVENT_MAP);
        addAlias(EVENT, VALUE);
        addMapping(INTERFACE, INTERFACE_MAP);
        addAlias(INTERFACE, KEY);
        registerPreProcessor(new CheckExclusiveAttributes(INTERFACE_ATTRIBUTES));
        registerPreProcessor(new CheckExclusiveAttributes(EVENT_ATTRIBUTES));
        registerPreProcessor(new CheckRequiredAttributes(INTERFACE_ATTRIBUTES));
        registerPreProcessor(new CheckRequiredAttributes(EVENT_ATTRIBUTES));
    }

    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        super.parseChild(element, parserContext, builder);    //To change body of overridden methods use File | Settings | File Templates.
    }

}
