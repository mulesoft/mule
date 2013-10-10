/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.routing;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;

/**
 * Enables an artifact to be matched for routing before actually routing to it
 */
public interface Matchable
{
    /**
     * Determines if the event should be processed
     *
     * @param message the current message to evaluate
     * @return true if the event should be processed by this router
     * @throws MuleException if the event cannot be evaluated
     */
    boolean isMatch(MuleMessage message) throws MuleException;
}
