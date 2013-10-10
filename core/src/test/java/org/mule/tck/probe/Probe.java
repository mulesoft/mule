/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.probe;

/**
 * A probe indicates whether the state of the system satisfies a given criteria
 */
public interface Probe
{

    /**
     * Indicates wheter or not the specified criteria was met or not.
     *
     * @return true if the criteria is satisfied.
     */
    boolean isSatisfied();

    /**
     * Describes the cause of the criteria failure for further analysis.
     *
     * @return the error message.
     */
    String describeFailure();
}
