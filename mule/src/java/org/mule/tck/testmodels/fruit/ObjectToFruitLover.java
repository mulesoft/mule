/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.tck.testmodels.fruit;

import org.mule.transformers.DefaultTransformer;
import org.mule.umo.transformer.TransformerException;


public class ObjectToFruitLover extends DefaultTransformer
{
    public ObjectToFruitLover()
    {
        this.setReturnClass(FruitLover.class);
        this.registerSourceType(String.class);
        this.registerSourceType(FruitLover.class);
    }

    public Object doTransform(Object src) throws TransformerException
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