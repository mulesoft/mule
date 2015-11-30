/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.debug;

import org.mule.tck.size.SmallTest;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

@SmallTest
public class ObjectFieldDebugInfoTestCase extends AbstractFieldDebugInfoTestCase<List<FieldDebugInfo>>
{

    public static final String FIELD_NAME = "foo";

    @Override
    protected List<FieldDebugInfo> getValue()
    {
        final SimpleFieldDebugInfo fieldDebugInfo = new SimpleFieldDebugInfo(FIELD_NAME, String.class, "test");
        return Collections.<FieldDebugInfo>singletonList(fieldDebugInfo);
    }

    @Override
    protected void createFieldDebugInfo(String name, Class type, List<FieldDebugInfo> value)
    {
        new ObjectFieldDebugInfo(name, type, value);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validatesNullValue() throws Exception
    {
        createFieldDebugInfo(FIELD_NAME, Map.class, null);
    }
}
