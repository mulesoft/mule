/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.xml;

import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

import java.util.ArrayList;
import java.util.List;

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

    private volatile String expression;

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
            Object result;
            if (src instanceof String)
            {
                Document doc = DocumentHelper.parseText((String)src);
                if (singleResult)
                {
                    result = doc.valueOf(expression);
                }
                else
                {
                    XPath xpath = doc.createXPath(expression);
                    // TODO handle non-list cases, see
                    // http://www.dom4j.org/apidocs/org/dom4j/XPath.html#evaluate(java.lang.Object)
                    List obj = (List)xpath.evaluate(doc);
                    result = new ArrayList(obj.size());
                    for (int i = 0; i < obj.size(); i++)
                    {
                        final Node node = (Node)obj.get(i);
                        ((List)result).add(node.getText());
                    }
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
}
