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

import org.mule.api.MuleContext;
import org.mule.api.transformer.DataType;

public class TransformerBuilder
{

    private DataType<?> returnDataType;
    private DataType<?> sourceDataType;
    private Object returnValue;
    private MuleContext muleContext;

    public TransformerBuilder from(DataType<?> returnDataType)
    {
        this.returnDataType = returnDataType;

        return this;
    }

    public TransformerBuilder to(DataType<?> sourceDataType)
    {
        this.sourceDataType = sourceDataType;

        return this;
    }

    public TransformerBuilder returning(Object returnValue)
    {
        this.returnValue = returnValue;

        return this;
    }

    public TransformerBuilder boundTo(MuleContext muleContext)
    {
        this.muleContext = muleContext;

        return this;
    }

    public TestTransformer build()
    {
        TestTransformer result = new TestTransformer();

        setUpTransformer(result);

        return result;
    }

    public TestConverter buildConverter(int weighting)
    {
        TestConverter result = new TestConverter();

        setUpTransformer(result);
        result.setPriorityWeighting(weighting);

        return result;
    }

    private void setUpTransformer(TestTransformer result)
    {
        result.registerSourceType(sourceDataType);
        result.setReturnDataType(returnDataType);
        result.setMuleContext(muleContext);
        result.setReturnValue(returnValue);
    }
}
