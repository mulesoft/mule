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
import org.mule.util.StringMessageUtils;

/**
 * <code>DefaultHandler</code> TODO (document class)
 *
 */
public class DefaultHandler extends AbstractExceptionHandler
{
    public DefaultHandler()
    {
        super();
        registerException(Throwable.class);
    }

    @Override
    public void processException(ErrorMessage message, Throwable t) throws HandlerException
    {
        String msg = LocaleMessage.defaultHandlerMessage();
        System.out.println(StringMessageUtils.getBoilerPlate(msg));
    }
}
