/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.i18n;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.endpoint.ImmutableEndpoint;
import org.mule.runtime.core.config.i18n.Message;
import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.core.util.ObjectUtils;
import org.mule.runtime.core.util.StringMessageUtils;

public class JmsMessages extends MessageFactory
{
    private static final JmsMessages factory = new JmsMessages();
    
    private static final String BUNDLE_PATH = getBundlePath("jms");
    
    public static Message connectorDoesNotSupportSyncReceiveWhenTransacted()
    {
        return factory.createMessage(BUNDLE_PATH, 2);
    }

    public static Message sessionShouldBeTransacted()
    {
        return factory.createMessage(BUNDLE_PATH, 4);
    }

    public static Message sessionShouldNotBeTransacted()
    {
        return factory.createMessage(BUNDLE_PATH, 5);
    }

    public static Message noMessageBoundForAck()
    {
        return factory.createMessage(BUNDLE_PATH, 6);
    }

    public static Message messageMarkedForRedelivery(MuleMessage message)
    {
        String messageDescription = (message == null) ? "[null message]" : message.getUniqueId();
        return factory.createMessage(BUNDLE_PATH, 7, messageDescription);
    }

    public static Message messageMarkedForRedelivery(MuleEvent event)
    {
        return messageMarkedForRedelivery(event.getMessage());
    }

    public static Message failedToCreateAndDispatchResponse(Object object)
    {
        return factory.createMessage(BUNDLE_PATH, 8, ObjectUtils.toString(object, "null"));
    }

    public static Message tooManyRedeliveries(String messageId, int times, int maxRedelivery, ImmutableEndpoint endpoint)
    {
        return factory.createMessage(BUNDLE_PATH, 11, messageId, times, maxRedelivery, endpoint.getEndpointURI(), endpoint.getConnector().getName());
    }

    public static Message invalidResourceType(Class<?> expectedClass, Object object)
    {
        Class<?> actualClass = null;
        if (object != null)
        {
            actualClass = object.getClass();
        }
        
        return factory.createMessage(BUNDLE_PATH, 12, StringMessageUtils.toString(expectedClass),
            StringMessageUtils.toString(actualClass));
    }

    public static Message checkTransformer(String string, Class<?> class1, String name)
    {
        return factory.createMessage(BUNDLE_PATH, 13, string, ClassUtils.getSimpleName(class1.getClass()),
            name);
    }
    
    public static Message noConnectionFactoryConfigured()
    {
        return factory.createMessage(BUNDLE_PATH, 14);
    }

    public static Message errorInitializingJndi()
    {
        return factory.createMessage(BUNDLE_PATH, 15);
    }

    public static Message errorCreatingConnectionFactory()
    {
        return factory.createMessage(BUNDLE_PATH, 16);
    }

    public static Message errorMuleMqJmsSpecification()
    {
        return factory.createMessage(BUNDLE_PATH, 17);
    }
}
