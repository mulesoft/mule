/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk 
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */
 
package org.mule.samples.errorhandler;

import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

/**
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class ExceptionBeanToErrorMessage extends AbstractTransformer
{
    public ExceptionBeanToErrorMessage()
    {
        registerSourceType(ExceptionBean.class);
    }

    /* (non-Javadoc)
     * @see org.mule.transformers.AbstractTransformer#doTransform(java.lang.Object)
     */
    public Object doTransform(Object src) throws TransformerException
    {
        try
        {
            return new ErrorMessage((ExceptionBean)src);
        }
        catch (InstantiationException e)
        {
            throw new TransformerException(this, e);
        }
    }

}
