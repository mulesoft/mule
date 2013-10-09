/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.factories;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;

import org.mule.api.AnnotatedObject;
import org.mule.api.processor.MessageProcessor;
import org.mule.routing.AbstractSelectiveRouter;
import org.mule.routing.MessageProcessorFilterPair;
import org.springframework.beans.factory.FactoryBean;

public abstract class AbstractSelectiveRouterFactoryBean implements FactoryBean, AnnotatedObject
{
    private MessageProcessor defaultProcessor;
    private Collection<MessageProcessorFilterPair> conditionalMessageProcessors;
    private final Map<QName, Object> annotations = new ConcurrentHashMap<QName, Object>();

    public AbstractSelectiveRouterFactoryBean()
    {
        super();
    }

    public void setDefaultRoute(MessageProcessorFilterPair conditionalProcessor)
    {
        defaultProcessor = conditionalProcessor.getMessageProcessor();
    }

    public void setRoutes(Collection<MessageProcessorFilterPair> conditionalMessageProcessors)
    {
        this.conditionalMessageProcessors = conditionalMessageProcessors;
    }

    public Object getObject() throws Exception
    {
        final AbstractSelectiveRouter router = newAbstractSelectiveRouter();
        router.setAnnotations(getAnnotations());
        router.setDefaultRoute(defaultProcessor);

        for (final MessageProcessorFilterPair mpfp : conditionalMessageProcessors)
        {
            router.addRoute(mpfp.getMessageProcessor(), mpfp.getFilter());
        }

        return router;
    }

    protected abstract AbstractSelectiveRouter newAbstractSelectiveRouter();

    public boolean isSingleton()
    {
        return true;
    }

    public final Object getAnnotation(QName name)
    {
        return annotations.get(name);
    }

    public final Map<QName, Object> getAnnotations()
    {
        return Collections.unmodifiableMap(annotations);
    }

    public synchronized final void setAnnotations(Map<QName, Object> newAnnotations)
    {
        annotations.clear();
        annotations.putAll(newAnnotations);
    }
}
