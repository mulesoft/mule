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

public final class LifecycleTransitionResult
{

    /** Transition successful **/
    public static final LifecycleTransitionResult OK = new LifecycleTransitionResult("ok");

    /** Request retry once other components initialised **/
    public static final LifecycleTransitionResult RETRY = new LifecycleTransitionResult("retry");

    private String name;

    private LifecycleTransitionResult(String name)
    {
        this.name = name;
    }

    public String toString()
    {
        return ClassUtils.getSimpleName(getClass()) + ": " + name;
    }

}
