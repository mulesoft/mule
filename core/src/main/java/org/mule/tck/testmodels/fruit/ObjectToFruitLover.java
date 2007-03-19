/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.testmodels.fruit;

import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

public class ObjectToFruitLover extends AbstractTransformer
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -6760497986912711312L;

    public ObjectToFruitLover()
    {
        this.setReturnClass(FruitLover.class);
        this.registerSourceType(String.class);
        this.registerSourceType(FruitLover.class);
    }

    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        if (src instanceof FruitLover)
        {
            return src;
        }
        else
        {
            return new FruitLover((String)src);
        }
    }

}
