/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.resolvers;


public class EntryPointResolverTestCase extends AbstractEntryPointResolverTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/integration/resolvers/entry-point-resolver-test.xml";
    }

    public void testArrayEntryPointResolver() throws Exception
    {
        doTest("array", new String[]{"hello", "world"}, "array");
    }

//    public void testCallableEntryPointResolver() throws Exception
//    {
//        doTest("callable", new Object(), "callable");
//    }
//
//    public void testCustomEntryPointResolver() throws Exception
//    {
//        doTest("custom", new Object(), "custom");
//    }
//
//    public void testMethodEntryPointResolver() throws Exception
//    {
//        doTest("method", new String(), "methodString");
//        doTest("method", new Integer(0), "methodInteger");
//    }
//
//    public void testNoArgumentsEntryPointResolver() throws Exception
//    {
//        doTest("no-arguments", new String(), "noArguments");
//    }
//
//    public void testPropertyEntryPointResolver() throws Exception
//    {
//        Map properties = new HashMap();
//        properties.put("propertyName", "property");
//        doTest("property", new Object(), "property", properties);
//    }
//
//    public void testReflectionEntryPointResolver() throws Exception
//    {
//        doTest("reflection", new Integer(0), "reflection");
//    }
//
//    public void testLegacyEntryPointResolvers() throws Exception
//    {
//        doTest("legacy", "hello world", "callable");
//    }
//
//    public void testOrderedEntryPointResolvers() throws Exception
//    {
//        doTest("ordered", new Integer(0), "methodInteger");
//    }

}
