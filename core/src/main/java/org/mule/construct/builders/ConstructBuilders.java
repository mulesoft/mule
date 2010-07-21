/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.construct.builders;

/**
 * A helper class for creating flow construct builders.
 */
public abstract class ConstructBuilders
{
    private ConstructBuilders()
    {
        throw new UnsupportedOperationException("do not instantiate");
    }

    public static SimpleServiceBuilder buildSimpleService()
    {
        return new SimpleServiceBuilder();
    }

}
