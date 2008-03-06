/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.lifecycle;

import org.mule.util.ClassUtils;

/**
 * Restrict possible results - only OK or a retry based on some throwable are currently allowed.
 */
public final class LifecycleTransitionResult
{

    public static final String OK_NAME = "ok";
    public static final String RETRY_NAME = "retry";


    /** Transition successful **/
    public static final LifecycleTransitionResult OK = new LifecycleTransitionResult(OK_NAME, true, null);

    private boolean ok;
    private Throwable throwable;
    private String name;

    private LifecycleTransitionResult(String name, boolean ok, Throwable throwable)
    {
        this.name = name;
        this.ok = ok;
        this.throwable = throwable;
    }

    public static LifecycleTransitionResult retry(Throwable throwable)
    {
        return new LifecycleTransitionResult(RETRY_NAME, false, throwable);
    }

    public boolean isOk()
    {
        return ok;
    }

    public Throwable getThrowable()
    {
        return throwable;
    }

    public String toString()
    {
        return ClassUtils.getSimpleName(getClass()) + ": " + name;
    }

}
