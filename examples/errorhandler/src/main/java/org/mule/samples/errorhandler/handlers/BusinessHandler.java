/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.errorhandler.handlers;

import org.mule.samples.errorhandler.AbstractExceptionHandler;
import org.mule.samples.errorhandler.ErrorMessage;
import org.mule.samples.errorhandler.HandlerException;
import org.mule.samples.errorhandler.LocaleMessage;
import org.mule.samples.errorhandler.exceptions.BusinessException;
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
