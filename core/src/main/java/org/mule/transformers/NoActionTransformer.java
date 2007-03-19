/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers;

import org.mule.umo.transformer.TransformerException;

/**
 * <code>NoActionTransformer</code> doesn't do any transformation on the source
 * object and returns the source as the result. This can be used to overload the
 * default transform for an endpoint.
 */
public final class NoActionTransformer extends AbstractTransformer
{

    public NoActionTransformer()
    {
        registerSourceType(Object.class);
        setReturnClass(Object.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.transformer.UMOTransformer#transform(java.lang.Object)
     */
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        return src;
    }

    // @Override
    public boolean isAcceptNull()
    {
        return true;
    }

}
