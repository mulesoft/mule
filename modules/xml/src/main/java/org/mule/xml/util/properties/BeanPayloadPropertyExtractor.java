/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.xml.util.properties;

import org.jaxen.JaxenException;
import org.jaxen.XPath;
import org.jaxen.javabean.JavaBeanXPath;

/** TODO */
public class BeanPayloadPropertyExtractor extends AbstractXPathPropertyExtractor
{
    public static final String NAME = "bean";

    protected XPath createXPath(String expression, Object object) throws JaxenException
    {
        expression = expression.replaceAll("[.]", "/");
        return new JavaBeanXPath(expression);
    }

    protected Object extractResultFromNode(Object result)
    {
        return ((org.jaxen.javabean.Element)result).getObject();
    }

    /** {@inheritDoc} */
    public String getName()
    {
        return NAME;
    }
}
