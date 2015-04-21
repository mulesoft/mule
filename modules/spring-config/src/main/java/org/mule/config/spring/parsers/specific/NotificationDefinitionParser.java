/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.api.context.notification.ClusterNodeNotificationListener;
import org.mule.api.context.notification.ComponentMessageNotificationListener;
import org.mule.api.context.notification.ConnectionNotificationListener;
import org.mule.api.context.notification.ConnectorMessageNotificationListener;
import org.mule.api.context.notification.CustomNotificationListener;
import org.mule.api.context.notification.EndpointMessageNotificationListener;
import org.mule.api.context.notification.ExceptionNotificationListener;
import org.mule.api.context.notification.ExceptionStrategyNotificationListener;
import org.mule.api.context.notification.ManagementNotificationListener;
import org.mule.api.context.notification.MessageProcessorNotificationListener;
import org.mule.api.context.notification.ModelNotificationListener;
import org.mule.api.context.notification.MuleContextNotificationListener;
import org.mule.api.context.notification.RegistryNotificationListener;
import org.mule.api.context.notification.RoutingNotificationListener;
import org.mule.api.context.notification.SecurityNotificationListener;
import org.mule.api.context.notification.ServiceNotificationListener;
import org.mule.api.context.notification.TransactionNotificationListener;
import org.mule.config.spring.parsers.PreProcessor;
import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;
import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.processors.CheckExclusiveAttributes;
import org.mule.config.spring.parsers.processors.CheckRequiredAttributes;
import org.mule.context.notification.AsyncMessageNotification;
import org.mule.context.notification.ComponentMessageNotification;
import org.mule.context.notification.ConnectionNotification;
import org.mule.context.notification.ConnectorMessageNotification;
import org.mule.context.notification.CustomNotification;
import org.mule.context.notification.EndpointMessageNotification;
import org.mule.context.notification.ExceptionNotification;
import org.mule.context.notification.ExceptionStrategyNotification;
import org.mule.context.notification.ManagementNotification;
import org.mule.context.notification.MessageProcessorNotification;
import org.mule.context.notification.ModelNotification;
import org.mule.context.notification.MuleContextNotification;
import org.mule.context.notification.PipelineMessageNotification;
import org.mule.context.notification.RegistryNotification;
import org.mule.context.notification.RoutingNotification;
import org.mule.context.notification.SecurityNotification;
import org.mule.context.notification.ServiceNotification;
import org.mule.context.notification.TransactionNotification;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

public class NotificationDefinitionParser extends ChildMapEntryDefinitionParser
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
    public static final String[][] ALL_ATTRIBUTES =
            new String[][]{
                    new String[]{EVENT}, new String[]{EVENT_CLASS},
                    new String[]{INTERFACE}, new String[]{INTERFACE_CLASS}};

    static
    {
        EVENT_MAP = new HashMap();
        EVENT_MAP.put("CONTEXT", MuleContextNotification.class.getName());
        EVENT_MAP.put("MODEL", ModelNotification.class.getName());
        EVENT_MAP.put("SERVICE", ServiceNotification.class.getName());
        EVENT_MAP.put("SECURITY", SecurityNotification.class.getName());
        EVENT_MAP.put("ENDPOINT-MESSAGE", EndpointMessageNotification.class.getName());
        EVENT_MAP.put("CONNECTOR-MESSAGE", ConnectorMessageNotification.class.getName());
        EVENT_MAP.put("COMPONENT-MESSAGE", ComponentMessageNotification.class.getName());
        EVENT_MAP.put("MANAGEMENT", ManagementNotification.class.getName());
        EVENT_MAP.put("MESSAGE-PROCESSOR", MessageProcessorNotification.class.getName());
        EVENT_MAP.put("EXCEPTION-STRATEGY", ExceptionStrategyNotification.class.getName());
        EVENT_MAP.put("CONNECTION", ConnectionNotification.class.getName());
        EVENT_MAP.put("REGISTRY", RegistryNotification.class.getName());
        EVENT_MAP.put("CUSTOM", CustomNotification.class.getName());
        EVENT_MAP.put("EXCEPTION", ExceptionNotification.class.getName());
        EVENT_MAP.put("TRANSACTION", TransactionNotification.class.getName());
        EVENT_MAP.put("ROUTING", RoutingNotification.class.getName());
        EVENT_MAP.put("PIPELINE-MESSAGE", PipelineMessageNotification.class.getName());
        EVENT_MAP.put("ASYNC-MESSAGE", AsyncMessageNotification.class.getName());

        INTERFACE_MAP = new HashMap();
        INTERFACE_MAP.put("CONTEXT", MuleContextNotificationListener.class.getName());
        INTERFACE_MAP.put("MODEL", ModelNotificationListener.class.getName());
        INTERFACE_MAP.put("SERVICE", ServiceNotificationListener.class.getName());
        INTERFACE_MAP.put("SECURITY", SecurityNotificationListener.class.getName());
        INTERFACE_MAP.put("MANAGEMENT", ManagementNotificationListener.class.getName());
        INTERFACE_MAP.put("MESSAGE-PROCESSOR", MessageProcessorNotificationListener.class.getName());
        INTERFACE_MAP.put("EXCEPTION-STRATEGY", ExceptionStrategyNotificationListener.class.getName());
        INTERFACE_MAP.put("CONNECTION", ConnectionNotificationListener.class.getName());
        INTERFACE_MAP.put("REGISTRY", RegistryNotificationListener.class.getName());
        INTERFACE_MAP.put("CUSTOM", CustomNotificationListener.class.getName());
        INTERFACE_MAP.put("ENDPOINT-MESSAGE", EndpointMessageNotificationListener.class.getName());
        INTERFACE_MAP.put("CONNECTOR-MESSAGE", ConnectorMessageNotificationListener.class.getName());
        INTERFACE_MAP.put("COMPONENT-MESSAGE", ComponentMessageNotificationListener.class.getName());
        INTERFACE_MAP.put("EXCEPTION", ExceptionNotificationListener.class.getName());
        INTERFACE_MAP.put("TRANSACTION", TransactionNotificationListener.class.getName());
        INTERFACE_MAP.put("ROUTING", RoutingNotificationListener.class.getName());
        INTERFACE_MAP.put("CLUSTER-NODE", ClusterNodeNotificationListener.class.getName());
        INTERFACE_MAP.put("PIPELINE-MESSAGE", PipelineMessageNotification.class.getName());
        INTERFACE_MAP.put("ASYNC-MESSAGE", AsyncMessageNotification.class.getName());
        
        // Deprecated
        EVENT_MAP.put("MESSAGE", EndpointMessageNotification.class.getName());

    }

    public NotificationDefinitionParser()
    {
        super("interfaceToType", INTERFACE_CLASS, EVENT_CLASS);
        addMapping(EVENT, EVENT_MAP);
        addAlias(EVENT, VALUE);
        addMapping(INTERFACE, INTERFACE_MAP);
        addAlias(INTERFACE, KEY);
        registerPreProcessor(new CheckExclusiveAttributes(INTERFACE_ATTRIBUTES));
        registerPreProcessor(new CheckExclusiveAttributes(EVENT_ATTRIBUTES));
        registerPreProcessor(new CheckRequiredAttributes(INTERFACE_ATTRIBUTES));
        registerPreProcessor(new CheckRequiredAttributes(EVENT_ATTRIBUTES));
        registerPreProcessor(new SetDefaults());
    }

    /**
     * If only one of event or interface is set, use it as default for the other
     */
    private class SetDefaults implements PreProcessor
    {

        public void preProcess(PropertyConfiguration config, Element element)
        {
            copy(element, INTERFACE, EVENT, EVENT_CLASS);
            copy(element, EVENT, INTERFACE, INTERFACE_CLASS);
        }

        private void copy(Element element, String from, String to, String blocker)
        {
            if (element.hasAttribute(from) && !element.hasAttribute(to) && !element.hasAttribute(blocker))
            {
                element.setAttribute(to, element.getAttribute(from));
            }
        }

    }

}
