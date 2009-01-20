/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import org.mule.api.service.ServiceException;
import org.mule.api.transformer.TransformerException;
import org.mule.message.ExceptionMessage;
import org.mule.transformer.AbstractTransformer;

/**
 * Test transformer that extracts the orginal payload of a pessage that failed and turns it into an
 * {@link org.mule.test.integration.exceptions.ErrorResponse}
 */
public class ExceptionToErrorResponseTransformer extends AbstractTransformer
{

    public ExceptionToErrorResponseTransformer()
    {
        super();
    }

    protected Object doTransform(Object src, String encoding) throws TransformerException
    {
        Object ret = src;
        Throwable t;
        if (src instanceof ExceptionMessage)
        {
            t = ((ExceptionMessage) src).getException();
            if (t instanceof ServiceException)
            {
                t = t.getCause();
            }
            ErrorResponse er = new ErrorResponse();
            er.setDescription(t.getMessage());
            Throwable cause = t.getCause();
            if (cause != null)
            {
                er.setRootCause(cause.getMessage());
            }
            ret = er;
        }
        return ret;
    }

}
