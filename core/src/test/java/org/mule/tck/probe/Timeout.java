/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.probe;

public class Timeout
{

    private final long duration;
    private final long start;

    public Timeout(long duration)
    {
        this.duration = duration;
        this.start = System.currentTimeMillis();
    }

    public boolean hasTimedOut()
    {
        final long now = System.currentTimeMillis();
        return (now - start) > duration;
    }
}
