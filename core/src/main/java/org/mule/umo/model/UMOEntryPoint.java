/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.model;

import org.mule.umo.UMOEventContext;

/**
 * <code>UMOEntryPoint</code> defines the current entry method on a component. If
 * the invoked method does not have a return value, a {@link org.mule.impl.VoidResult}
 * is returned.
 */
public interface UMOEntryPoint
{
    Object invoke(Object component, UMOEventContext context) throws Exception;
}
