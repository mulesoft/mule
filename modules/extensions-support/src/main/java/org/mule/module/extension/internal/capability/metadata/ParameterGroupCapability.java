/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.capability.metadata;

import org.mule.extension.introspection.Parameter;
import org.mule.module.extension.internal.introspection.ParameterGroup;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * A metadata capability to specify that a certain group of {@link Parameter}s
 * are grouped. This grouping is transparent and is not reflected on the introspection
 * model because it's implementation specific.
 * <p/>
 * This capability provides the necessary metadata for the runtime to handle
 * those parameters accordingly.
 * <p/>
 * This capability gives access to a list of {@link ParameterGroup} instances through the
 * {@link #getGroups()} method.
 * <p/>
 * This class is immutable
 *
 * @since 3.7.0
 */
public final class ParameterGroupCapability
{

    private final List<ParameterGroup> groups;

    public ParameterGroupCapability(List<ParameterGroup> groups)
    {
        this.groups = ImmutableList.copyOf(groups);
    }

    public List<ParameterGroup> getGroups()
    {
        return groups;
    }
}
