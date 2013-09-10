/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.expression;

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

}


