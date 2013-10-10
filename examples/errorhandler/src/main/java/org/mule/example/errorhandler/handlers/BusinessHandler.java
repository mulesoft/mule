/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

    protected void processException(ErrorMessage message, Throwable t) throws HandlerException
    {
        String msg = LocaleMessage.businessHandlerMessage();
        System.out.println(StringMessageUtils.getBoilerPlate(msg));
    }
}
