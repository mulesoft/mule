/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
