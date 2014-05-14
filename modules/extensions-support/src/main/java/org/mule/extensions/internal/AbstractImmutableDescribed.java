/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.internal;

import org.mule.extensions.introspection.api.Described;
import org.mule.util.Preconditions;

import org.apache.commons.lang.StringUtils;

abstract class AbstractImmutableDescribed implements Described
{

    private final String name;
    private final String description;

    protected AbstractImmutableDescribed(String name, String description)
    {
        Preconditions.checkArgument(!StringUtils.isBlank(name), "Name attribute cannot be null or blank");
        Preconditions.checkArgument(!StringUtils.isBlank(description), "Description attribute cannot be null or blank");

        this.name = name;
        this.description = description;
    }

    @Override
    public final String getName()
    {
        return name;
    }

    @Override
    public final String getDescription()
    {
        return description;
    }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }
}
