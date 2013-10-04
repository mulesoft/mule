/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
