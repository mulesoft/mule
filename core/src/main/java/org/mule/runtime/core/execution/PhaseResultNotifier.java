/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.execution;

/**
 *  Notifier used by {@link MessageProcessPhase} in order to
 *  define the result of the phase execution
 */
public interface PhaseResultNotifier
{

    /**
     * This method must be called when the phase complete successfully
     */
    public void phaseSuccessfully();

    /**
     * This method must be called when the phase consume the message and the chain should not continue processing the message.
     * If this method is called then phaseSuccessfully must not be called
     */
    public void phaseConsumedMessage();

    /**
     * This message must be called when a phase execution throw an exception
     *
     * @param reason exception that represents the failure in the phase
     */
    public void phaseFailure(Exception reason);
}
