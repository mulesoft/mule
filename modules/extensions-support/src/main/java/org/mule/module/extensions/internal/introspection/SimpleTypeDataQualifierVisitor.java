/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.introspection;

import org.mule.extensions.introspection.DataQualifier;
import org.mule.extensions.introspection.DataQualifierVisitor;

/**
 * Base implementation for a {@link DataQualifierVisitor} which adds the new
 * method {@link #onSimpleType()} which is invoked by all the {@link DataQualifier}s
 * which refer to a simple type. Those would be:
 * <p/>
 * <ul>
 * <li>{@link #onBoolean()}</li>
 * <li>{@link #onInteger()}</li>
 * <li>{@link #onDouble()}</li>
 * <li>{@link #onDecimal()}</li>
 * <li>{@link #onString()}</li>
 * <li>{@link #onLong()}</li>
 * <li>{@link #onEnum()}</li>
 * </ul>
 * <p/>
 * All other qualifiers delegate into {@link #defaultOperation()} by default, but they
 * can be overridden at will
 *
 * @since 3.7.0
 */
public abstract class SimpleTypeDataQualifierVisitor extends BaseDataQualifierVisitor
{

    protected abstract void onSimpleType();

    @Override
    public void onBoolean()
    {
        onSimpleType();
    }

    @Override
    public void onInteger()
    {
        onSimpleType();
    }

    @Override
    public void onDouble()
    {
        onSimpleType();
    }

    @Override
    public void onDecimal()
    {
        onSimpleType();
    }

    @Override
    public void onString()
    {
        onSimpleType();
    }

    @Override
    public void onLong()
    {
        onSimpleType();
    }

    @Override
    public void onEnum()
    {
        onSimpleType();
    }
}
