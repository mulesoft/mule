/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.debug;

import org.mule.util.StringUtils;

import java.util.List;

import static org.mule.util.Preconditions.checkArgument;

/**
 * Represents an execution frame with the set of variables available and a name.
 *
 * @since 3.8.0
 */
public class DebuggerFrame
{
    private List<FieldDebugInfo<?>> variables;
    private String name;

    public DebuggerFrame(List<FieldDebugInfo<?>> variables, String name)
    {
        checkArgument(variables != null, "Variables cannot be null.");
        checkArgument(!StringUtils.isEmpty(name), "Name cannot be empty.");
        this.variables = variables;
        this.name = name;
    }

    /**
     * Returns the list of variables active in this frame
     * @return The list of variables
     */
    public List<FieldDebugInfo<?>> getVariables()
    {
        return variables;
    }

    /**
     * The name of the frame
     * @return The name
     */
    public String getName()
    {
        return name;
    }
}
