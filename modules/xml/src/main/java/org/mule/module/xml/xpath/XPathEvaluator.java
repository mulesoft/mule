/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.xpath;

import org.mule.api.MuleEvent;
import org.mule.module.xml.util.NamespaceManager;

import java.util.Map;

import org.w3c.dom.Node;

/**
 * A simple component of evaluation XPath expressions in full conformance of the XPath 2.0 spec
 * and basic conformance of the XPath 3.0 (everything except features which require schema awareness
 * and high order functions).
 * <p/>
 * All implementations are require to support the ability to pass parameters into the query. For that reason,
 * all the evaluation methods will require the current {@link MuleEvent} to be passed on, in order to
 * resolve those parameters against the flow variables.
 * <p/>
 * All implementations are required to be thread-safe
 *
 * @since 3.6.0
 */
public interface XPathEvaluator
{

    /**
     * Evaluates the {@code xpathExpression} over the {@code input}
     * and returns the evaluation as a {@link String}.
     * <p/>
     * If {@code xpathExpression} contains parameters, they will be resolved
     * against the flow variables in {@code event}
     *
     * @param xpathExpression the xpathExpression to be evaluated
     * @param input           a {@link Node}
     * @param event           the current {@link MuleEvent}.
     * @return the result of the evaluation as a String
     */
    String evaluate(String xpathExpression, Node input, MuleEvent event);

    /**
     * Evaluates the {@code xpathExpression} over the {@code input}
     * and returns the evaluation as a type in concordance with {@code returnType}.
     * <p/>
     * If {@code xpathExpression} contains parameters, they will be resolved
     * against the flow variables in {@code event}
     *
     * @param xpathExpression the xpathExpression to be evaluated
     * @param input           a {@link Node}
     * @param returnType      a {@link XPathReturnType} that will be used to decide the return type of the evaluation
     * @param event           the current {@link MuleEvent}.
     * @return the result of the evaluation in concordance with {@code returnType}
     */
    Object evaluate(String xpathExpression, Node input, XPathReturnType returnType, MuleEvent event);

    /**
     * Registers the given namespaces so that they can be recognized during evaluation
     *
     * @param namespaces a {@link Map} in which the key is a namespace prefix and the value is its URI as
     *                   a {@link String}
     */
    void registerNamespaces(Map<String, String> namespaces);

    /**
     * Registers the namespaces in {@code namespaceManager}
     *
     * @param namespaceManager a {@link NamespaceManager}
     */
    void registerNamespaces(NamespaceManager namespaceManager);

    /**
     * Returns a {@link Map} with the registered namespaces. The key of the map
     * is the namespace prefix and the value is its URI as a {@link String}
     *
     * @return a {@link Map}
     */
    Map<String, String> getRegisteredNamespaces();

}
