/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.xml.expression;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

/**
 * Will select the text of a single node based on the property name
 */
public class XPathNodeExpressionEvaluator extends XPathExpressionEvaluator
{
    public static final String NAME = "xpath-node";

    protected Object extractResultFromNode(Object result)
    {
        if(result instanceof Element)
        {
            ((Element)result).detach();
            return DocumentHelper.createDocument((Element)result);
        }
        return result;
    }

    /** {@inheritDoc} */
    public String getName()
    {
        return NAME;
    }
}