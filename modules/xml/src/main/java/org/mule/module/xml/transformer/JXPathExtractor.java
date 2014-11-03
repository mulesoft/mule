/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.transformer;

import org.mule.api.MuleContext;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.xml.util.NamespaceManager;
import org.mule.transformer.AbstractTransformer;
import org.mule.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.jxpath.JXPathContext;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;
import org.dom4j.XPath;

/**
 * The JXPathExtractor is a simple transformer that evaluates an xpath expression
 * against the given bean and that returns the result. <p/> By default, a single
 * result will be returned. If multiple values are expected, set the
 * {@link #singleResult} property to <code>false</code>. In this case a
 * {@link List} of values will be returned. Note the property is currently ignored
 * for non-String/XML payloads.
 *
 * @deprecated This feature is deprecated and will be removed in Mule 4.0. Use MEL for extracting information
 * out of a Java Object or the xpath3() MEL function in the case of an XML document
 */
@Deprecated
public class JXPathExtractor extends AbstractTransformer
{
    public static final String OUTPUT_TYPE_NODE = "NODE";

    public static final String OUTPUT_TYPE_XML = "XML";

    public static final String OUTPUT_TYPE_VALUE = "VALUE";

    private volatile String expression;

    private volatile String outputType;

    private volatile Map<String, String> namespaces;

    private volatile boolean singleResult = true;

    private NamespaceManager namespaceManager;

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
        try
        {
            namespaceManager = muleContext.getRegistry().lookupObject(NamespaceManager.class);
        }
        catch (RegistrationException e)
        {
            throw new ExpressionRuntimeException(CoreMessages.failedToLoad("NamespaceManager"), e);
        }
    }

    /**
     * Template method where deriving classes can do any initialisation after the
     * properties have been set on this transformer
     *
     * @throws org.mule.api.lifecycle.InitialisationException
     *
     */
    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        if (namespaceManager != null)
        {
            if (namespaces == null)
            {
                namespaces = new HashMap<String, String>(namespaceManager.getNamespaces());
            }
            else
            {
                namespaces.putAll(namespaceManager.getNamespaces());
            }
        }
    }

    /**
     * Evaluate the expression in the context of the given object and returns the
     * result. If the given object is a string, it assumes it is an valid xml and
     * parses it before evaluating the xpath expression.
     */
    @Override
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        try
        {
            Object result = null;
            if (src instanceof String)
            {
                Document doc = DocumentHelper.parseText((String) src);

                XPath xpath = doc.createXPath(expression);
                if (namespaces != null)
                {
                    xpath.setNamespaceURIs(namespaces);
                }

                // This is the way we always did it before, so keep doing it that way
                // as xpath.evaluate() will return non-string results (like Doubles)
                // for some scenarios.
                if (outputType == null && singleResult)
                {
                    return xpath.valueOf(doc);
                }

                // TODO handle non-list cases, see
                //http://www.dom4j.org/apidocs/org/dom4j/XPath.html#evaluate(java.lang.Object)
                Object obj = xpath.evaluate(doc);
                if (obj instanceof List)
                {
                    for (int i = 0; i < ((List<?>) obj).size(); i++)
                    {
                        final Node node = (Node) ((List<?>) obj).get(i);
                        result = add(result, node);

                        if (singleResult)
                        {
                            break;
                        }
                    }
                }
                else
                {
                    result = add(result, obj);
                }

            }
            else
            {
                JXPathContext context = JXPathContext.newContext(src);
                result = context.getValue(expression);
            }
            return result;
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }

    }

    @SuppressWarnings("unchecked")
    private Object add(Object result, Object value)
    {
        Object formattedResult = getResult(value);
        if (singleResult)
        {
            return formattedResult;
        }
        else
        {
            if (result == null)
            {
                result = new ArrayList<Object>();
            }

            ((List<Object>) result).add(formattedResult);
        }
        return result;
    }

    private Object getResult(Object value)
    {
        Object result = null;
        if (StringUtils.contains(OUTPUT_TYPE_VALUE, outputType) || outputType == null)
        {
            if (value instanceof Node)
            {
                result = ((Node) value).getText();
            }
            else
            {
                // this maintains backward compat with previous 2.1.x versions.
                result = value.toString();
            }
        }
        else if (StringUtils.contains(OUTPUT_TYPE_XML, outputType))
        {
            if (value instanceof Node)
            {
                result = ((Node) value).asXML();
            }
            else
            {
                throw new IllegalStateException("XPath expression output must be a Node to output as XML. Expression type was: " + value.getClass());
            }
        }
        else if (StringUtils.contains(OUTPUT_TYPE_NODE, outputType))
        {
            result = value;
        }
        return result;
    }

    /**
     * @return Returns the expression.
     */
    public String getExpression()
    {
        return expression;
    }

    /**
     * @param expression The expression to set.
     */
    public void setExpression(String expression)
    {
        this.expression = expression;
    }

    /**
     * Should a single value be returned.
     *
     * @return value
     */
    public boolean isSingleResult()
    {
        return singleResult;
    }

    /**
     * If multiple results are expected from the {@link #expression} evaluation, set
     * this to false.
     *
     * @param singleResult flag
     */
    public void setSingleResult(boolean singleResult)
    {
        this.singleResult = singleResult;
    }

    public String getOutputType()
    {
        return outputType;
    }

    public void setOutputType(String outputEncoding)
    {
        this.outputType = outputEncoding;
    }

    public Map<String, String> getNamespaces()
    {
        return namespaces;
    }

    public void setNamespaces(Map<String, String> namespaceURIs)
    {
        this.namespaces = namespaceURIs;
    }
}
