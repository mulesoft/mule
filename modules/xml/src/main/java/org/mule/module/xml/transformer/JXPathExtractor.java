/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.xml.transformer;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;
import org.mule.util.StringUtils;

import java.util.ArrayList;
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
 */
public class JXPathExtractor extends AbstractTransformer
{
    public static final String OUTPUT_TYPE_NODE = "NODE";

    public static final String OUTPUT_TYPE_XML = "XML";

    public static final String OUTPUT_TYPE_VALUE = "VALUE";

    private volatile String expression;
    
    private volatile String outputType;
    
    private volatile Map namespaces;

    private volatile boolean singleResult = true;

    /**
     * Evaluate the expression in the context of the given object and returns the
     * result. If the given object is a string, it assumes it is an valid xml and
     * parses it before evaluating the xpath expression.
     */
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
                
                // TODO handle non-list cases, see
                //http://www.dom4j.org/apidocs/org/dom4j/XPath.html#evaluate(java.lang.Object)
                Object obj = xpath.evaluate(doc);
                if (obj instanceof List)
                {
                    for (int i = 0; i < ((List) obj).size(); i++)
                    {
                        final Node node = (Node) ((List) obj).get(i);
                        result = add(result, node);
                        
                        if (singleResult)
                        {
                            if (logger.isWarnEnabled())
                            {
                                logger.warn("There are multiple Nodes returned. But only one record is returned because singleResult is true");
                            }
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
    
    private Object add(Object result, Object value)
    {
        if (singleResult)
        {
            if (StringUtils.contains(OUTPUT_TYPE_VALUE, outputType) || outputType == null)
            {
                result = ((Node) value).getText();
            }
            else if (StringUtils.contains(OUTPUT_TYPE_XML, outputType))
            {
                result = ((Node) value).asXML();
            }
            else if (StringUtils.contains(OUTPUT_TYPE_NODE, outputType))
            {
                result = value;
            }
        }
        else
        {
            if (result == null)
            {
                result = new ArrayList();
            }
            if (StringUtils.contains(OUTPUT_TYPE_NODE, outputType))
            {
                ((List) result).add(value);
            }
            else if (StringUtils.contains(OUTPUT_TYPE_XML, outputType))
            {
                ((List) result).add(((Node) value).asXML());
            }
            else if (StringUtils.contains(OUTPUT_TYPE_VALUE, outputType) || outputType == null)
            {
                ((List) result).add(((Node) value).getText());
            }
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

    public Map getNamespaces()
    {
        return namespaces;
    }

    public void setNamespaces(Map namespaceURIs)
    {
        this.namespaces = namespaceURIs;
    }
    
}
