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
package org.mule.transformers.xml;

import org.apache.commons.jxpath.JXPathContext;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

/**
 * The JXPathExtractor is a simple transformer that evaluates an xpath
 * expression against the given bean and that returns the result.
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class JXPathExtractor extends AbstractTransformer
{

    private String expression;

    /**
     * Evaluate the expression in the context of the given object and returns
     * the result. If the given object is a string, it assumes it is an valid
     * xml and parses it before evaluating the xpath expression.
     */
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        try {
            Object o = null;
            if (src instanceof String) {
                Document doc = DocumentHelper.parseText((String) src);
                o = doc.valueOf(expression);
            } else {
                JXPathContext context = JXPathContext.newContext(src);
                o = context.getValue(expression);
            }
            return o;
        } catch (Exception e) {
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
}
