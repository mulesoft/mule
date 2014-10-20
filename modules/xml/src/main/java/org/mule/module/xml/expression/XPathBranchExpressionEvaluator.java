/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.expression;

import org.mule.module.xml.el.XPath3Function;

/**
 * @deprecated This expression evaluator is deprecated and will be removed in Mule 4. Use {@link XPath3Function} instead
 */
@Deprecated
public class XPathBranchExpressionEvaluator extends XPathExpressionEvaluator
{
    public static final String NAME = "xpath-branch";

    @Override
    protected Object extractResultFromNode(Object result)
    {
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    protected String getDeprecationMessage()
    {
        return "The xpath-branch: expression evaluator has been deprecated in Mule 3.6.0 and will be removed in 4.0. Please use the xpath3() MEL function instead";
    }

}


