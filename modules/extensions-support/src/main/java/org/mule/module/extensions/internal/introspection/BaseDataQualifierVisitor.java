/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.introspection;

import org.mule.extensions.introspection.DataQualifierVisitor;

/**
 * Base implementation of {@link DataQualifierVisitor}
 * in which all methods are implemented and delegate into {@link #defaultOperation()}. In this way,
 * you can only implement the methods you care about and have a central point to determine which logic
 * to apply on the cases you don't specifically care about
 * <p/>
 * This default implementation of {@link #defaultOperation()} is a no-op
 *
 * @since 3.7.0
 */
public class BaseDataQualifierVisitor implements DataQualifierVisitor
{

    @Override
    public void onBoolean()
    {
        defaultOperation();
    }

    @Override
    public void onInteger()
    {
        defaultOperation();
    }

    @Override
    public void onDouble()
    {
        defaultOperation();
    }

    @Override
    public void onDecimal()
    {
        defaultOperation();
    }

    @Override
    public void onString()
    {
        defaultOperation();
    }

    @Override
    public void onLong()
    {
        defaultOperation();
    }

    @Override
    public void onEnum()
    {
        defaultOperation();
    }

    @Override
    public void onDateTime()
    {
        defaultOperation();
    }

    @Override
    public void onPojo()
    {
        defaultOperation();
    }

    @Override
    public void onList()
    {
        defaultOperation();
    }

    @Override
    public void onMap()
    {
        defaultOperation();
    }

    @Override
    public void onOperation()
    {
        defaultOperation();
    }

    protected void defaultOperation()
    {
        //no op
    }
}
