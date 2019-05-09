/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;

/**
 * Dummy exception listener. Used in cases when a Mule overriden exception listener isn't applied in a {@link org.springframework.jms.connection.CachingConnectionFactory}.
 */
public class NullExceptionListener implements ExceptionListener
{

    @Override
    public void onException(JMSException exception)
    {
        // Do nothing.
    }
}
