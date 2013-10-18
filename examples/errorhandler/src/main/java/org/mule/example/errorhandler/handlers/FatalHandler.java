/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.errorhandler.handlers;

import org.mule.api.lifecycle.FatalException;
import org.mule.example.errorhandler.ErrorMessage;
import org.mule.example.errorhandler.HandlerException;
import org.mule.example.errorhandler.LocaleMessage;
import org.mule.util.StringMessageUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>FatalBehaviour</code> TODO (document class)
 */
public class FatalHandler extends DefaultHandler
{
    /** logger used by this class */
    private static final Log logger = LogFactory.getLog(FatalHandler.class);

    public FatalHandler()
    {
        super();
        registerException(FatalException.class);
    }

    public void processException(ErrorMessage message, Throwable t) throws HandlerException
    {
        String msg = LocaleMessage.fatalHandlerMessage();
        System.out.println(StringMessageUtils.getBoilerPlate(msg));
        logger.fatal(LocaleMessage.fatalHandlerException(t), t);
    }
}
