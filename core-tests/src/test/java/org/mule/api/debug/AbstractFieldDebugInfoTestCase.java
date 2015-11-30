/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.debug;

import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

public abstract class AbstractFieldDebugInfoTestCase<T> extends AbstractMuleTestCase
{

    @Test(expected = IllegalArgumentException.class)
    public void validatesNullFieldName() throws Exception
    {
        createFieldDebugInfo(null, String.class, getValue());
    }

    protected abstract T getValue();

    protected abstract void createFieldDebugInfo(String name, Class type, T value);

    @Test(expected = IllegalArgumentException.class)
    public void validatesEmptyFieldName() throws Exception
    {
        createFieldDebugInfo("", String.class, getValue());
    }
}
