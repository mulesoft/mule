/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.scan.annotations;

@Marker("ClassLevel")
public class SampleClassWithAnnotations
{
    @Marker("FieldLevel")
    private String myField;

    private int anotherNonAnnotatedField;

    @Marker("MethodLevel / Main")
    public static void main(@Marker("ParamLevel")
                            @MultiMarker(value = "ParamLevel", param1 = "12", param2 = "abc")
                            String[] args) throws Exception
    {
        // no-op
    }

    @Override
    @Marker("MethodLevel / toString")
    public String toString()
    {
        return super.toString();
    }

    public void nonAnnotatedMethod()
    {
        // no-op
    }
}
