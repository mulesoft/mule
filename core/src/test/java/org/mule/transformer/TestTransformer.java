/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformer;

import org.mule.api.transformer.TransformerException;

public class TestTransformer extends AbstractTransformer
{

    private boolean executed;
    private Object returnValue;

    @Override
    public Object transform(Object src, String enc) throws TransformerException
    {
        executed = true;
        return super.transform(src, enc);
    }

    @Override
    protected Object doTransform(Object src, String enc) throws TransformerException
    {
        return returnValue;
    }

    public boolean wasExecuted()
    {
        return executed;
    }

    public void setReturnValue(Object returnValue)
    {
        this.returnValue = returnValue;
    }
}
