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
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ErrorMessageToExceptionBean extends AbstractTransformer
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 2186094860534344459L;

    public ErrorMessageToExceptionBean()
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
        return ((ErrorMessage)src).getException();
    }
}