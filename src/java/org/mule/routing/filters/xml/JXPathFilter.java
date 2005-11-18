/*
 * $Header$
 * $Revision$
 * $Date$
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

import org.apache.commons.jxpath.AbstractFactory;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.mule.umo.UMOFilter;
import org.mule.umo.UMOMessage;

import java.util.Iterator;
import java.util.Map;

/**
 * <code>JXPathFilter</code> evaluates an XPath expression against an Xml
 * document or bean and returns true if the result is expected.
 * 
 * @author <a href="mailto:S.Vanmeerhaege@gfdi.be">Vanmeerhaeghe Stéphane</a>
 * @version $Revision$
 */
public class JXPathFilter implements UMOFilter
{

    private static final Log LOGGER = LogFactory.getLog(JXPathFilter.class);

    private String expression;

    private String value;

    private Map namespaces = null;

    private Map contextProperties = null;

    private AbstractFactory factory;

    private boolean lenient = true;

    public JXPathFilter() {

    }

    public JXPathFilter(String expression) {
        this.expression = expression;
    }

    public boolean accept(UMOMessage obj)
    {
        return accept(obj.getPayload());
    }
    
    private boolean accept(Object obj)
    {
        if (obj == null) {
            return false;
        }
        if (expression == null) {
            LOGGER.warn("Expression for JXPathFilter is not set");
            return false;
        }

        if (value == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Value for JXPathFilter is not set : true by default");
            }
            value = Boolean.TRUE.toString();
        }

        boolean res = false;

        try {
            Object o = null;

            if (obj instanceof String) {
                Document doc = DocumentHelper.parseText((String) obj);
                o = doc.valueOf(expression);
	    } else if (obj instanceof org.dom4j.Document) {
                Document doc = (Document)obj;
                o = doc.valueOf(expression);
            } else {
		if (LOGGER.isDebugEnabled()) {
		    LOGGER.debug("Passing object of type " + 
			    obj.getClass().toString() + " to JXPathContext");
		}
                JXPathContext context = JXPathContext.newContext(obj);
                initialise(context);
                o = context.getValue(expression);
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("JXPathFilter Expression result='" + o + "' -  Expected value='" + value + "'");
            }

            if (o != null) {
                res = value.equals(o.toString());
            } else {
                res = false;
                LOGGER.warn("JXPathFilter Expression result is null (" + expression + ")");
            }
        } catch (Exception e) {
            LOGGER.warn("JXPathFilter cannot evaluate expression (" + expression + ") :" + e.getMessage(), e);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("JXPathFilter accept object  : " + res);
        }
        return res;

    }

    protected void initialise(JXPathContext context)
    {
        Map.Entry entry = null;
        if(namespaces!=null) {
            for (Iterator iterator = namespaces.entrySet().iterator(); iterator.hasNext();) {
                entry = (Map.Entry) iterator.next();
                context.registerNamespace(entry.getKey().toString(), entry.getValue().toString());
            }
        }

        if(contextProperties!=null) {
            for (Iterator iterator = namespaces.entrySet().iterator(); iterator.hasNext();) {
                entry = (Map.Entry) iterator.next();
                context.setValue(entry.getKey().toString(), entry.getValue());
            }
        }

        if(factory!=null) {
            context.setFactory(factory);
        }
        context.setLenient(lenient);
    }

    /**
     * @return XPath expression
     */
    public String getExpression()
    {
        return expression;
    }

    /**
     * @param expression The XPath expression
     */
    public void setExpression(String expression)
    {
        this.expression = expression;
    }

    /**
     * @return The expected result value of the XPath expression
     */
    public String getValue()
    {
        return value;
    }

    /**
     * @param value The expected result value of the XPath expression
     */
    public void setValue(String value)
    {
        this.value = value;
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
