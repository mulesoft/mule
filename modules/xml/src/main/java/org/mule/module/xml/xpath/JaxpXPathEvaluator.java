/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.xpath;

import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.MuleEvent;
import org.mule.api.MuleRuntimeException;
import org.mule.module.xml.i18n.XmlMessages;
import org.mule.module.xml.util.NamespaceManager;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathVariableResolver;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.w3c.dom.Node;

/**
 * This is the preferred base implementation of {@link XPathEvaluator}. Because it's
 * based on the JAXP API (JSR-206), it's ideal for keeping a common code base which
 * can work with different engines, as long as they implement that API.
 * <p>
 * This base class contains all of the logic necessary to comply with the
 * {@link XPathEvaluator} contract. Implementations only need to implement
 * {@link #createXPathFactory()} in order to provide the {@link XPathFactory}
 * implementation it wishes to use.
 * <p>
 * Another important feature of this implementation is that it caches compiled
 * versions of executed expressions to provide better performance. Expressions that haven't
 * been used for more than a minute are automatically evicted.
 * <p>
 * In addition to the {@link #registerNamespaces(Map)} and {@link #registerNamespaces(NamespaceManager)}
 * methods, this implementation also provides out of the box support for the standard namespaces
 * defined in {@link XPathNamespaceContext}
 * <p>
 * In order to allow binding expression parameters to flow variables, this class also
 * implements the {@link XPathVariableResolver} interface. Because this class caches
 * compiled expressions which might be executed concurrently in different threads, we need a
 * way to correlate different {@link MuleEvent} instances to each invocation of the {@link #resolveVariable(QName)}
 * method. To do that, it uses a {@link ThreadLocal} in the {@link #evaluationEvent} attribute, so that
 * we can determine the corresponding event for each thread evaluating an XPath expression. Notice that
 * because xpath evaluation is an operation that happens in RAM memory (basically because the DOM {@link Node}
 * needs to be completely loaded), we can use a {@link ThreadLocal} without risking failure if this is
 * executed in a non-blocking environment.
 *
 * @since 3.6.0
 */
public abstract class JaxpXPathEvaluator implements XPathEvaluator, XPathVariableResolver
{

    private static final int MIN_IDLE_XPATH_EXPRESSIONS = 1;
    private static final int MAX_IDLE_XPATH_EXPRESSIONS = 32;
    private static final int MAX_ACTIVE_XPATH_EXPRESSIONS = MAX_IDLE_XPATH_EXPRESSIONS;

    private final XPathFactory xpathFactory;
    private final Map<String, String> prefixToNamespaceMap = new HashMap<>();
    private final ThreadLocal<MuleEvent> evaluationEvent = new ThreadLocal<>();
    private final LoadingCache<String, GenericObjectPool<XPathExpression>> xpathExpressionPoolCache = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .removalListener(new RemovalListener<String, GenericObjectPool<XPathExpression>>()
            {
                @Override
                public void onRemoval(RemovalNotification<String, GenericObjectPool<XPathExpression>> notification)
                {
                    notification.getValue().clear();

                }
            })
            .build(new CacheLoader<String, GenericObjectPool<XPathExpression>>()
            {
                @Override
                public GenericObjectPool<XPathExpression> load(String key) throws Exception
                {
                    return getXPathExpressionPool(key);
                }
            });



    private NamespaceContext namespaceContext;

    public JaxpXPathEvaluator()
    {
        xpathFactory = createXPathFactory();
        namespaceContext = newNamespaceContext();
    }

    /**
     * Returns the {@link XPathFactory} to be used when
     * compiling expressions
     *
     * @return a {@link XPathFactory}
     */
    protected abstract XPathFactory createXPathFactory();

    /**
     * {@inheritDoc}
     */
    @Override
    public String evaluate(String xpathExpression, Node input, MuleEvent event)
    {
        return (String) evaluate(xpathExpression, input, XPathReturnType.STRING, event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object evaluate(String xpathExpression, Node input, XPathReturnType returnType, MuleEvent event)
    {
        XPathExpression xpath = null;
        GenericObjectPool <XPathExpression> xpathExpressionPool = null;
        try
        {
            evaluationEvent.set(event);
            xpathExpressionPool = xpathExpressionPoolCache.get(xpathExpression);
            xpath = xpathExpressionPool.borrowObject();
            Object result = xpath.evaluate(input, returnType.toQName());
            return result;
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(XmlMessages.failedToProcessXPath(xpathExpression), e);
        }
        finally
        {
            evaluationEvent.remove();
            try
            {
                if (xpathExpressionPool != null && xpath != null)
                {
                    xpathExpressionPool.returnObject(xpath);
                }
            }
            catch (Exception e)
            {
                throw new MuleRuntimeException(XmlMessages.failedToProcessXPath(xpathExpression), e);
            }

        }
    }

    /**
     * Resolves the given variable against the flow variables
     * in the {@link MuleEvent} held by {@link #evaluationEvent}
     *
     * @param variableName the variable name
     * @return the variable value. Might be {@code null}
     */
    @Override
    public Object resolveVariable(QName variableName)
    {
        MuleEvent event = evaluationEvent.get();
        if (event != null)
        {
            return event.getFlowVariable(variableName.getLocalPart());
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerNamespaces(Map<String, String> namespaces)
    {
        checkArgument(namespaces != null, "cannot register null namespaces");
        prefixToNamespaceMap.putAll(namespaces);
        namespaceContext = newNamespaceContext();
        xpathExpressionPoolCache.invalidateAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerNamespaces(NamespaceManager namespaceManager)
    {
        checkArgument(namespaceManager != null, "cannot register a null namespace manager");
        registerNamespaces(namespaceManager.getNamespaces());
    }

    /**
     * {@inheritDoc}
     * Returns an immutable map with the current registered namespaces.
     * The returned map will not reflect any changes performed afterwards.
     */
    @Override
    public Map<String, String> getRegisteredNamespaces()
    {
        return ImmutableMap.copyOf(prefixToNamespaceMap);
    }

    private GenericObjectPool<XPathExpression> getXPathExpressionPool(String expression)
    {
        GenericObjectPool genericPool = new GenericObjectPool(new XPathExpressionFactory(xpathFactory, expression, namespaceContext, this));
        genericPool.setMaxActive(MAX_ACTIVE_XPATH_EXPRESSIONS);
        genericPool.setMaxIdle(MAX_IDLE_XPATH_EXPRESSIONS);
        genericPool.setMinIdle(MIN_IDLE_XPATH_EXPRESSIONS);
        return genericPool;
    }


    protected NamespaceContext newNamespaceContext()
    {
        return new XPathNamespaceContext(prefixToNamespaceMap);
    }
}
