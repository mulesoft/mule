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
 * <code>UMOEntryPointResolver</code> resolves a method to call on the given
 * UMODescriptor when an event is received for the component
 */
public interface UMOEntryPointResolver
{
    InvocationResult invoke(Object component, UMOEventContext context) throws Exception;
}
