/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.resolvers;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class EntryPointResolverTestCase extends AbstractEntryPointResolverTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/resolvers/entry-point-resolver-test.xml";
    }

    @Test
    public void testArrayEntryPointResolverOnModel() throws Exception
    {
        doTest("array", new String[]{"hello", "world"}, "array");
    }
    
    @Test
    public void testArrayEntryPointResolverOnComponent() throws Exception
    {
        doTest("array2", new String[]{"hello", "world"}, "array");
    }

    @Test
    public void testCallableEntryPointResolverOnModel() throws Exception
    {
        doTest("callable", new Object(), "callable");
    }

    @Test
    public void testCallableEntryPointResolverOnComponent() throws Exception
    {
        doTest("callable2", new Object(), "callable");
    }

    @Test
    public void testCustomEntryPointResolverOnModel() throws Exception
    {
        doTest("custom", new Object(), "custom");
    }

    @Test
    public void testCustomEntryPointResolverOnComponent() throws Exception
    {
        doTest("custom2", new Object(), "custom");
    }

    @Test
    public void testMethodEntryPointResolverOnModel() throws Exception
    {
        doTest("method", new String(), "methodString");
        doTest("method", new Integer(0), "methodInteger");
    }

    @Test
    public void testMethodEntryPointResolverOnComponent() throws Exception
    {
        doTest("method2", new String(), "methodString");
        doTest("method2", new Integer(0), "methodInteger");
    }

    @Test
    public void testNoArgumentsEntryPointResolverOnModel() throws Exception
    {
        doTest("no-arguments", new String(), "noArguments");
    }

    @Test
    public void testNoArgumentsEntryPointResolverOnComponent() throws Exception
    {
        doTest("no-arguments2", new String(), "noArguments");
    }

    @Test
    public void testPropertyEntryPointResolverOnModel() throws Exception
    {
        Map properties = new HashMap();
        properties.put("propertyName", "property");
        doTest("property", new Object(), "property", properties);
    }

    @Test
    public void testPropertyEntryPointResolverOnComponent() throws Exception
    {
        Map properties = new HashMap();
        properties.put("propertyName", "property");
        doTest("property2", new Object(), "property", properties);
    }

    @Test
    public void testReflectionEntryPointResolverOnModel() throws Exception
    {
        doTest("reflection", new Object[]{new Integer(0), new String("String")}, "reflection");
    }

    @Test
    public void testReflectionEntryPointResolverOnComponent() throws Exception
    {
        doTest("reflection2", new Object[]{new Integer(0), new String("String")}, "reflection");
    }

    @Test
    public void testLegacyEntryPointResolversOnModel() throws Exception
    {
        doTest("legacy", "hello world", "callable");
    }

    @Test
    public void testLegacyEntryPointResolversOnComponent() throws Exception
    {
        doTest("legacy2", "hello world", "callable");
    }

    @Test
    public void testReflectionEntryPointResolverWithNullElementInArray() throws Exception
    {
        // see MULE-3565
        
        // This first case causes an exception in the flow because the ReflectionEntryPointResolver
        // will take the argument types literally and it doesn't know how to handle the null as class
        doTest("reflection", new Object[] { new Integer(42), null }, "{NullPayload}");
        
        doTest("array", new String[] { "hello", null, "world" }, "array");
    }
}
