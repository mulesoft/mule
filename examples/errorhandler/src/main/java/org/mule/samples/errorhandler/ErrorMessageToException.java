/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.errorhandler;

import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

/**
 * The <code>ErrorMessageToException</code> transformer extracts and returns 
 * the exception encapsulated by the ErrorMessage message payload.
 */
public class ErrorMessageToException extends AbstractTransformer
{

    public ErrorMessageToException()
    {
        registerSourceType(ErrorMessage.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transformers.AbstractTransformer#doTransform(java.lang.Object)
     */
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        try
        {
            return ((ErrorMessage)src).getException().toException();
        }
        catch (InstantiationException e)
        {
            throw new TransformerException(this, e);
        }
    }

}
