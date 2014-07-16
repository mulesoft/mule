/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.errorhandler;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;

/**
 * The <code>ErrorMessageToExceptionBean</code> transformer returns
 * the exception bean encapsulated by the ErrorMessage message payload.
 */
public class ErrorMessageToExceptionBean extends AbstractTransformer
{
    public ErrorMessageToExceptionBean()
    {
        registerSourceType(DataTypeFactory.create(ErrorMessage.class));
    }

    @Override
    public Object doTransform(Object src, String outputEncoding) throws TransformerException
    {
        return ((ErrorMessage)src).getException();
    }
}
