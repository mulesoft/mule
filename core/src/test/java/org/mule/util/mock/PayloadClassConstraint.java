/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
