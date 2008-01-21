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

/**
 * Builder that is used to build instances of {@link MuleContext}. Implementing
 * classes are stateful and should provide public chainable setters for configuring
 * the builder instance and no public getters.
 */
public interface MuleContextBuilder
{

    /**
     * Builds a new {@link MuleContext} instance using configured builder instance.
     * Does not initialise or start MuleContext, only constructs the instance.
     * 
     * @return
     */
    MuleContext buildMuleContext();

}
