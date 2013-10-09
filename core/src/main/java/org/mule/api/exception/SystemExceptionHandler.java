/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.exception;



/**
 * Take some action when a system exception has occurred (i.e., there was no message in play when the exception occurred).
 */
public interface SystemExceptionHandler extends ExceptionHandler
{
    /**
     * Take some action when a system exception has occurred (i.e., there was no message in play when the exception occurred).
     * 
     * @param exception which occurred
     * @param rollbackMethod will be called if transactions are not used in order to achieve atomic message delivery 
     */
    void handleException(Exception exception, RollbackSourceCallback rollbackMethod);

    /**
     * Take some action when a system exception has occurred (i.e., there was no message in play when the exception occurred).
     * 
     * @param exception which occurred
     */
    void handleException(Exception exception);
}


