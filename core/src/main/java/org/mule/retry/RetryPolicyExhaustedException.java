/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.retry;

import org.mule.api.lifecycle.FatalException;
import org.mule.config.i18n.Message;

/** 
 * This exception is thrown when a Retry policy has made all the retry attempts 
 * it wants to make and is still failing.
 */
public class RetryPolicyExhaustedException extends FatalException
{
    /** Serial version */
    private static final long serialVersionUID = 3300563235465630595L;

    public RetryPolicyExhaustedException(Message message, Object component)
    {
        super(message, component);
    }

    public RetryPolicyExhaustedException(Message message, Throwable cause, Object component)
    {
        super(message, cause, component);
    }

    public RetryPolicyExhaustedException(Throwable cause, Object component)
    {
        super(cause, component);
    }
}
