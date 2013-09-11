/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.devkit.capability;


/**
 * This interface is implemented for every {@link org.mule.api.annotations.Module}
 * annotated class, to dynamically query what its capabilities are.
 */
public interface Capabilities
{

    /**
     * Returns true if this module implements such capability
     * 
     * @param capability The capability to test for
     * @return True if it does, false otherwise
     */
    boolean isCapableOf(ModuleCapability capability);
}
