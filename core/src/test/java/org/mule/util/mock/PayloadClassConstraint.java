/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.mock;

import org.mule.api.MuleMessage;

import com.mockobjects.constraint.Constraint;

/**
 * TODO
 */
public class PayloadClassConstraint implements Constraint
{
    private Class type;

    public PayloadClassConstraint(Class type)
    {
        this.type = type;
    }

    public boolean eval(Object o)
    {
        return ((MuleMessage) o).getPayload().getClass().equals(type);
    }
}