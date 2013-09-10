/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.errorhandler.handlers;

import org.mule.example.errorhandler.AbstractExceptionHandler;
import org.mule.example.errorhandler.ErrorMessage;
import org.mule.example.errorhandler.HandlerException;
import org.mule.example.errorhandler.LocaleMessage;
import org.mule.example.errorhandler.exceptions.BusinessException;
import org.mule.util.StringMessageUtils;

/**
 * <code>BusinessHandler</code>
 */
public class BusinessHandler extends AbstractExceptionHandler
{
    public BusinessHandler()
    {
        super();
        registerException(BusinessException.class);
    }

    @Override
    protected void processException(ErrorMessage message, Throwable t) throws HandlerException
    {
        String msg = LocaleMessage.businessHandlerMessage();
        System.out.println(StringMessageUtils.getBoilerPlate(msg));
    }
}
