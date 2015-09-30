/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.debug;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

@SmallTest
public class ObjectDebugInfoTestCase extends AbstractMuleTestCase
{

    public static final String FIELD_NAME = "someFieldName";
    public static final String FIELD_VALUE = "foo";

    @Test(expected = IllegalArgumentException.class)
    public void validatesNullFields() throws Exception
    {
        new ObjectDebugInfo(Object.class, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validatesEmptyFields() throws Exception
    {
        new ObjectDebugInfo(Object.class, Collections.<FieldDebugInfo>emptyList());
    }

    @Test
    public void validatesDuplicatedFieldNames() throws Exception
    {
        final List<FieldDebugInfo> fields = new ArrayList<>();
        fields.add(new FieldDebugInfo(FIELD_NAME, String.class, FIELD_VALUE));
        fields.add(new FieldDebugInfo(FIELD_NAME, String.class, FIELD_VALUE));

        try
        {
            new ObjectDebugInfo(Object.class, fields);
        }
        catch (IllegalArgumentException e)
        {
            assertThat(e.getMessage(), containsString(FIELD_NAME));
        }
    }
}
