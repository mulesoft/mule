/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import org.mule.api.registry.MuleRegistry;

/** 
 * A handle to the Mule Registry.  We should make no assumptions about the location of the actual Registry
 * implementation.  It might be simply a singleton object in the same JVM, or it might be in another JVM or 
 * even running remotely on another machine.
 */
public class RegistryContext
{
    public static MuleRegistry getRegistry()
    {
        return MuleServer.getMuleContext().getRegistry();
    }
}
