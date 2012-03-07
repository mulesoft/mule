/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el.mvel;

import org.mule.api.MuleMessage;

import org.mvel2.integration.VariableResolver;

class PayloadVariableResolver implements VariableResolver
{

    private static final long serialVersionUID = -3710025393528711515L;

    protected MuleMessage message;

    public PayloadVariableResolver(MuleMessage message)
    {
        this.message = message;
    }

    @Override
    public void setValue(Object value)
    {
        message.setPayload(value);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void setStaticType(Class type)
    {
    }

    @Override
    public Object getValue()
    {
        return message.getPayload();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Class getType()
    {
        return Object.class;
    }

    @Override
    public String getName()
    {
        return null;
    }

    @Override
    public int getFlags()
    {
        return 0;
    }

}
