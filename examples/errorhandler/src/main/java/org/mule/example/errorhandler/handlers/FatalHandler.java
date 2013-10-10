/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
