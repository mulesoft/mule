/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.retry;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;

import java.util.Map;

/**
 * The RetryContext is used to store any data which carries over from 
 * attempt to attempt such as response messages.
 */
public interface RetryContext
{
    String FAILED_RECEIVER = "failedReceiver";
    String FAILED_DISPATCHER = "failedDispatcher";
    String FAILED_REQUESTER = "failedRequester";

    /**
     * @return a read-only meta-info map or an empty map, never null.
     */
    Map<Object, Object> getMetaInfo();

    MuleMessage[] getReturnMessages();

    MuleMessage getFirstReturnMessage();

    void setReturnMessages(MuleMessage[] returnMessages);

    void addReturnMessage(MuleMessage result);

    String getDescription();

    MuleContext getMuleContext();

    /**
     * The most recent failure which prevented the context from validating the connection. Note that the method may
     * return null. Instead, the {@link #isOk()} should be consulted first.
     *
     * @return last failure or null
     */
    Throwable getLastFailure();

    /**
     * Typically called by validation logic to mark no problems with the current connection. Additionally,
     * clears any previous failure set.
     */
    void setOk();

    /**
     * Typically called by validation logic to mark a problem and an optional root cause.
     *
     * @param lastFailure the most recent failure, can be null
     */
    void setFailed(Throwable lastFailure);

    /**
     * Note that it's possible for an implementation to return false and have no failure specified, thus
     * the subsequent {@link #getLastFailure()} may return null.
     *
     * @return true if no problems detected before
     */
    boolean isOk();
}
