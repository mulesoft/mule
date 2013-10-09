/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.weblogic;

import org.mule.transport.jms.DefaultJmsTopicResolver;
import org.mule.transport.jms.Jms102bSupport;
import org.mule.transport.jms.JmsConnector;
import org.mule.util.ClassUtils;

import java.lang.reflect.Method;

import javax.jms.Destination;
import javax.jms.Queue;
import javax.jms.Topic;

/**
 * Weblogic-specific JMS topic resolver. Will use reflection and
 * a vendor API to detect topics.
 */
public class WeblogicJmsTopicResolver extends DefaultJmsTopicResolver
{
    /**
     * Cached empty class array, used in the no-args reflective method call.
     */
    protected static final Class[] PARAMETER_TYPES_NONE = new Class[0];

    /**
     * Create an instance of the resolver.
     *
     * @param connector owning connector
     */
    public WeblogicJmsTopicResolver(final JmsConnector connector)
    {
        super(connector);
    }


    /**
     * For Weblogic 8.x (JMS 1.0.2b) will use Weblogic-specific API call to test for topic.
     * For Weblogic 9.x and later (JMS 1.1) this call is not required due to the unified
     * messaging domains.
     *
     * @param destination a jms destination to test
     * @return {@code true} if the destination is a topic
     */
    public boolean isTopic(final Destination destination)
    {
        // don't check the invariants, we already handle Weblogic's case here

        boolean topic = destination instanceof Topic;

        if (topic && destination instanceof Queue &&
            getConnector().getJmsSupport() instanceof Jms102bSupport)
        {
            try
            {
                Method topicMethod = ClassUtils.getPublicMethod(destination.getClass(), "isTopic", PARAMETER_TYPES_NONE);

                topic = (Boolean) topicMethod.invoke(destination);
            }
            catch (Exception e)
            {
                logger.warn(e.getMessage()); 
            }
        }
        return topic;
    }

}
