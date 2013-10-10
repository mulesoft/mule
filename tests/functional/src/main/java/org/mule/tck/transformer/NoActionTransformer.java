/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.transformer;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;

/**
 * <code>NoActionTransformer</code> doesn't do any transformation on the source
 * object and returns the source as the result. This can be used to overload the
 * default transform for an endpoint.
 */
public final class NoActionTransformer extends AbstractTransformer
{

    public NoActionTransformer()
    {
        registerSourceType(DataTypeFactory.OBJECT);
        setReturnDataType(DataTypeFactory.OBJECT);
    }

    @Override
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        return src;
    }

    @Override
    public boolean isAcceptNull()
    {
        return true;
    }

}
