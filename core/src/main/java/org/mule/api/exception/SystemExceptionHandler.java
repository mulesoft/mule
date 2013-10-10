/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.exception;


import org.mule.routing.filters.WildcardFilter;

/**
 * Take some action when a system exception has occurred (i.e., there was no message in play when the exception occurred).
 */
public interface SystemExceptionHandler
{
    /**
     * Take some action when a system exception has occurred (i.e., there was no message in play when the exception occurred).
     * 
     * @param exception which occurred
     */
    void handleException(Exception exception);

    /**
     * Returns the filter that given an exception class will determine if a
     * transaction should be committed or not.
     *
     * @return the exception filter configured for commit of transactions or
     *         null if there is no filter.
     */
    WildcardFilter getCommitTxFilter();

    /**
     * Returns the filter that given an exception class will determine if a
     * transaction should be rollbacked or not.
     *
     * @return the exception filter configured for rollback of transactions or
     *         null if there is no filter.
     */
    WildcardFilter getRollbackTxFilter();
}


