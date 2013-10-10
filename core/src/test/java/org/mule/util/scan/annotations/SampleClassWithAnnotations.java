/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
