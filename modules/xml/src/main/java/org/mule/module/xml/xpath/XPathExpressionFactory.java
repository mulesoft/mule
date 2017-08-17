/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.xml.xpath;

import org.apache.commons.pool.BasePoolableObjectFactory;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathVariableResolver;

class XPathExpressionFactory extends BasePoolableObjectFactory <XPathExpression>
{

    private final String expression;
    private final XPathFactory xPathFactory;
    private final NamespaceContext namespaceContext;
    private final XPathVariableResolver variableResolver;

    public XPathExpressionFactory(XPathFactory xPathFactory, String expression, NamespaceContext namespaceContext, XPathVariableResolver variableResolver)
    {
        this.expression = expression;
        this.xPathFactory = xPathFactory;
        this.namespaceContext = namespaceContext;
        this.variableResolver = variableResolver;
    }

    @Override
    public XPathExpression makeObject() throws Exception
    {
        return newXPath().compile(expression);
    }

    private  XPath newXPath()
    {
        XPath xpath = xPathFactory.newXPath();
        xpath.setNamespaceContext(namespaceContext);
        xpath.setXPathVariableResolver(variableResolver);
        return xpath;
    }

}
