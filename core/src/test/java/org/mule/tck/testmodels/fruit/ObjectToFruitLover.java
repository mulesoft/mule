/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.fruit;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;

public class ObjectToFruitLover extends AbstractTransformer
{

    public ObjectToFruitLover()
    {
        this.setReturnDataType(DataTypeFactory.create(FruitLover.class));
        this.registerSourceType(DataTypeFactory.STRING);
        this.registerSourceType(DataTypeFactory.create(FruitLover.class));
    }

    @Override
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        if (src instanceof FruitLover)
        {
            return src;
        }
        else
        {
            return new FruitLover((String) src);
        }
    }

}
