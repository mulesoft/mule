/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.sftp.notification;

import org.mule.api.exception.RollbackSourceCallback;
import org.mule.api.exception.SystemExceptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionListener implements SystemExceptionHandler
{
    private static final Logger logger = LoggerFactory.getLogger(ExceptionListener.class);

    public void handleException(Exception e)
    {
        logger.debug(e.getLocalizedMessage());
    }

    public void handleException(Exception exception, RollbackSourceCallback rollbackMethod)
    {
        handleException(exception);
    }
}
