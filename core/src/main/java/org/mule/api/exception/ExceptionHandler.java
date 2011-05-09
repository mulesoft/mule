/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.exception;


/**
 * Take some action when a messaging exception has occurred (i.e., there was a message in play when the exception occurred).
 */
public interface ExceptionHandler 
{
    /**
     * Redeliver the source message again after it has been handled by this exception handler?
     */
    boolean isRedeliver();
}


