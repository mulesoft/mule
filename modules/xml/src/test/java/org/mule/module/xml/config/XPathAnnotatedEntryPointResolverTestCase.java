/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.config;

import org.mule.api.MuleEventContext;
import org.mule.api.config.MuleProperties;
import org.mule.api.model.InvocationResult;
import org.mule.api.transport.PropertyScope;
import org.mule.impl.model.resolvers.AnnotatedEntryPointResolver;
import org.mule.tck.AbstractMuleTestCase;

import java.util.Map;

import org.dom4j.Document;

public class XPathAnnotatedEntryPointResolverTestCase extends AbstractMuleTestCase
{
    public static final String TEST_PAYLOAD = "<foo><bar>bar</bar></foo>";

    public void testAnnotatedMethod() throws Exception
    {
        AnnotatedEntryPointResolver resolver = new AnnotatedEntryPointResolver();
        AnnotatedComponent component = new AnnotatedComponent();
        MuleEventContext context = getTestEventContext(TEST_PAYLOAD);
        //Since AnnotatedComponent2 has two annotated methods we need to set the method to call
        context.getMessage().setProperty(MuleProperties.MULE_METHOD_PROPERTY, "doStuff", PropertyScope.INVOCATION);
        InvocationResult result = resolver.invoke(component, context);
        assertEquals(result.getState(), InvocationResult.STATE_INVOKED_SUCESSFUL);
        assertTrue(result.getResult() instanceof Map);
        Map map = (Map)result.getResult();
        assertEquals(2, map.size());
        assertTrue(map.get("foo") instanceof Document);
        assertEquals("bar", map.get("bar"));
    }
}
