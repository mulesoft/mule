/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.outbound;

import org.mule.api.MuleEvent;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.exception.RollbackSourceCallback;
import org.mule.api.exception.SystemExceptionHandler;
import org.mule.exception.AbstractExceptionListener;

public class TestExceptionStrategy extends AbstractExceptionListener implements MessagingExceptionHandler, SystemExceptionHandler
    {

        public static Exception exception;
        public static MuleEvent event;
        
        @Override
        public void handleException(Exception exception, RollbackSourceCallback rollbackMethod)
        {
            this.exception = exception;
            
        }

        @Override
        public void handleException(Exception exception)
        {
            this.exception = exception;
        }

        @Override
        public MuleEvent handleException(Exception exception, MuleEvent event)
        {
            this.event = event;
            this.exception = exception;
            return event;
        }
        
    }