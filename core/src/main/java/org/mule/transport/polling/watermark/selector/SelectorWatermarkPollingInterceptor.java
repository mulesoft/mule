/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.polling.watermark.selector;

import org.mule.api.MuleEvent;
import org.mule.api.config.ConfigurationException;
import org.mule.transport.polling.watermark.Watermark;
import org.mule.transport.polling.watermark.WatermarkPollingInterceptor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of
 * {@link org.mule.transport.polling.watermark.WatermarkPollingInterceptor} that uses
 * a {@link WatermarkSelector} to return the new watermark value.
 * 
 * @since 3.5.0
 */
public class SelectorWatermarkPollingInterceptor extends WatermarkPollingInterceptor
{

    private static final Logger logger = LoggerFactory.getLogger(SelectorWatermarkPollingInterceptor.class);

    private final WatermarkSelector selector;
    private final String selectorExpression;

    public SelectorWatermarkPollingInterceptor(Watermark watermark,
                                               WatermarkSelector selector,
                                               String selectorExpression)
    {
        super(watermark);
        this.selector = selector;
        this.selectorExpression = selectorExpression;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     * <p>
     * If the payload is a {@link Collection}, then that collection is copied and
     * iterated passing all values evaluated through the selector. This is so because
     * not only different kinds of collections can be traversed in unpredictable
     * ways, but also collections are often copied before being iterated in which
     * case we have no interception point.
     * </p>
     * <p>
     * If thepayload is an {@link Iterator} or an {@link Iterable}, then dynamic
     * proxies are generated so that we can intercept all values an evaluate them
     * through the selector. <b>Notice that if the {@link Iterable} or
     * {@link Iterator} are not fully consumed, the unretrieved values will not be
     * received by the {@link WatermarkSelector}
     * </p>
     */
    @Override
    @SuppressWarnings("unchecked")
    public MuleEvent prepareRouting(MuleEvent sourceEvent, MuleEvent event) throws ConfigurationException
    {
        event = super.prepareRouting(sourceEvent, event);
        Object payload = event.getMessage().getPayload();
        final WatermarkSelector selector = new WatermarkSelectorWrapper(this.selector,
            this.selectorExpression, event);

        if (payload instanceof Collection)
        {
            // consume early since the user could consume this collection in
            // unpredictable ways. He could even not consume it completely at all
            Collection<Object> copy = new ArrayList<Object>(((Collection<Object>) payload));
            for (Object object : copy)
            {
                selector.acceptValue(object);
            }
        }
        if (payload instanceof Iterator)
        {
            event.getMessage().setPayload(this.proxy((Iterator<Object>) payload, selector));
        }
        else if (payload instanceof Iterable)
        {
            event.getMessage().setPayload(this.proxy((Iterable<Object>) payload, selector));
        }
        else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(String.format(
                    "Poll executing with payload of class %s but selector can only handle Iterator and Iterable. Watermark will not be updated",
                    payload.getClass().getCanonicalName()));
            }
        }

        return event;
    }

    @SuppressWarnings("unchecked")
    private Iterator<Object> proxy(final Iterator<Object> iterator, final WatermarkSelector selector)
    {
        return (Iterator<Object>) Proxy.newProxyInstance(iterator.getClass().getClassLoader(),
            new Class[]{Iterator.class}, new InvocationHandler()
            {

                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
                {
                    if (method.getName().equals("next") && (args == null || args.length == 0))
                    {
                        Object value = iterator.next();
                        selector.acceptValue(value);

                        return value;
                    }
                    else
                    {
                        return method.invoke(iterator, args);
                    }
                }
            });
    }

    @SuppressWarnings("unchecked")
    private Iterable<Object> proxy(final Iterable<Object> iterable, final WatermarkSelector selector)
    {
        return (Iterable<Object>) Proxy.newProxyInstance(iterable.getClass().getClassLoader(),
            new Class[]{Iterable.class}, new InvocationHandler()
            {

                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
                {
                    if (method.getName().equals("iterator") && (args == null || args.length == 0))
                    {
                        return proxy(iterable.iterator(), selector);
                    }
                    else
                    {
                        return method.invoke(iterable, args);
                    }
                }
            });
    }
}
