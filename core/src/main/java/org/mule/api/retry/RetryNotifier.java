/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.retry;

/**
 * This interface is a callback that allows actions to be performed after each retry attempt, 
 * such as firing notifications, logging, etc.
 */
public interface RetryNotifier
{
    /** Called each time a retry attempt fails. */
    public void onFailure(RetryContext context, Throwable e);

    /** Called when a retry attempt finally suceeds. */
    public void onSuccess(RetryContext context);
}
