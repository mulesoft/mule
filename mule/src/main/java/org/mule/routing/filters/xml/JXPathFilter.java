/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.routing.filters.xml;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.jxpath.AbstractFactory;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.mule.umo.UMOFilter;
import org.mule.umo.UMOMessage;
import org.mule.util.StringMessageUtils;

/**
 * <code>JXPathFilter</code> evaluates an XPath expression against a W3C Document,
 * XML string, or Java bean and returns true if the result is as expected.
 *
 * @author <a href="mailto:S.Vanmeerhaege@gfdi.be">Vanmeerhaeghe St?phane</a>
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 * @version $Revision$
 */
public class JXPathFilter implements UMOFilter {

    protected transient Log logger = LogFactory.getLog(getClass());

    private String expression;

    private String expectedValue;

    private Map namespaces = null;

    private Map contextProperties = null;

    private AbstractFactory factory;

    private boolean lenient = true;

    public JXPathFilter() {
        super();
    }

    public JXPathFilter(String expression) {
        this.expression = expression;
    }

    public JXPathFilter(String expression, String expectedValue) {
        this.expression = expression;
        this.expectedValue = expectedValue;
    }

    public boolean accept(UMOMessage obj) {
        return accept(obj.getPayload());
    }

    private boolean accept(Object obj) {

        if (obj == null) {
            logger.warn("Applying JXPathFilter to null object.");
            return false;
        }
        if (expression == null) {
            logger.warn("Expression for JXPathFilter is not set.");
            return false;
        }
        if (expectedValue == null) {

            // Handle the special case where the expected value really is null.
            if (expression.endsWith("= null") || expression.endsWith("=null")) {
                expectedValue = "null";
                expression = expression.substring(0, expression.lastIndexOf("="));
            }
            else {
                logger.info("Expected value for JXPathFilter is not set, using 'true' by default");
                expectedValue = Boolean.TRUE.toString();
            }
        }

        Object xpathResult = null;
        boolean accept = false;

        // Payload is a DOM Document
        if (obj instanceof Document) {
            xpathResult = ((Document) obj).valueOf(expression);
        }
        // Payload is a String of XML
        else if (obj instanceof String) {
            try {
                Document doc = DocumentHelper.parseText((String) obj);
                xpathResult = doc.valueOf(expression);
            } catch (DocumentException e) {
                logger.warn("JXPathFilter unable to parse XML document: " + e.getMessage(), e);
                logger.debug("XML = " + StringMessageUtils.truncate((String) obj, 200, false));
                return false;
            }
        }
        // Payload is a Java object
        else {
            logger.debug("Passing object of type " + obj.getClass().getName() + " to JXPathContext");
            JXPathContext context = JXPathContext.newContext(obj);
            initialise(context);
            xpathResult = context.getValue(expression);
        }

        logger.debug("JXPathFilter Expression result = '" + xpathResult
                        + "' -  Expected value = '" + expectedValue + "'");

        // Compare the XPath result with the expected result.
        if (xpathResult != null) {
            accept = xpathResult.toString().equals(expectedValue);
        }
        else {
            // A null result was actually expected.
            if (expectedValue.equals("null")) {
                accept = true;
            }
            // A null result was not expected, something probably went wrong.
            else {
                logger.warn("JXPathFilter expression evaluates to null: " + expression);
            }
        }
        logger.debug("JXPathFilter accept object  : " + accept);
        return accept;
    }

    /**
     * Initializes the JXPathContext based on any relevant properties set for the
     * filter.
     * @param the JXPathContext to initialize
     */
    protected void initialise(JXPathContext context) {
        Map.Entry entry = null;
        if (namespaces != null) {
            for (Iterator iterator = namespaces.entrySet().iterator(); iterator.hasNext();) {
                entry = (Map.Entry) iterator.next();
                context.registerNamespace(entry.getKey().toString(), entry
                        .getValue().toString());
            }
        }

        if (contextProperties != null) {
            for (Iterator iterator = namespaces.entrySet().iterator(); iterator.hasNext();) {
                entry = (Map.Entry) iterator.next();
                context.setValue(entry.getKey().toString(), entry.getValue());
            }
        }

        if (factory != null) {
            context.setFactory(factory);
        }
        context.setLenient(lenient);
    }

    /**
     * @return XPath expression
     */
    public String getExpression() {
        return expression;
    }

    /**
     * @param expression
     *            The XPath expression
     */
    public void setExpression(String expression) {
        this.expression = expression;
    }

    /**
     * @return The expected result value of the XPath expression
     */
    public String getExpectedValue() {
        return expectedValue;
    }

    /**
     * Sets the expected result value of the XPath expression
     */
    public void setExpectedValue(String expectedValue) {
        this.expectedValue = expectedValue;
    }

    /**
     * @return The expected result value of the XPath expression
     * @deprecated Use <code>getExpectedValue()</code>.
     */
    public String getValue() {
        return getExpectedValue();
    }

    /**
     * Sets the expected result value of the XPath expression
     * @deprecated Use <code>setExpectedValue(String expectedValue)</code>.
     */
    public void setValue(String value) {
        setExpectedValue(value);
    }

    public Map getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(Map namespaces) {
        this.namespaces = namespaces;
    }

    public Map getContextProperties() {
        return contextProperties;
    }

    public void setContextProperties(Map contextProperties) {
        this.contextProperties = contextProperties;
    }

    public AbstractFactory getFactory() {
        return factory;
    }

    public void setFactory(AbstractFactory factory) {
        this.factory = factory;
    }

    public boolean isLenient() {
        return lenient;
    }

    public void setLenient(boolean lenient) {
        this.lenient = lenient;
    }
}
