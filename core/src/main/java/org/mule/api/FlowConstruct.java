/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api;

import java.beans.ExceptionListener;

import org.mule.api.lifecycle.LifecycleState;

public interface FlowConstruct extends NamedObject {
    /**
     * The exception strategy to use to handle exceptions in the Mule component.
     * 
     * @return the exception strategy to use. If none has been set a default will be used.
     */
    ExceptionListener getExceptionListener();

    MuleContext getMuleContext();

    LifecycleState getLifecycleState();

}
