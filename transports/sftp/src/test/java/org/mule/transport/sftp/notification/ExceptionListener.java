
package org.mule.transport.sftp.notification;

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

}
