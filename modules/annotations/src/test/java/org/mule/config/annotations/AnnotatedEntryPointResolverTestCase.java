/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.annotations;

import org.mule.api.MuleEventContext;
import org.mule.api.model.InvocationResult;
import org.mule.component.simple.EchoComponent;
import org.mule.impl.model.resolvers.AnnotatedEntryPointResolver;
import org.mule.tck.AbstractMuleTestCase;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;

public class AnnotatedEntryPointResolverTestCase extends AbstractMuleTestCase
{
    public static final String TEST_PAYLOAD = "<foo><bar>Hello</bar></foo>";

    public void testAnnotatedMethod() throws Exception
    {
        AnnotatedEntryPointResolver resolver = new AnnotatedEntryPointResolver();
        resolver.setMuleContext(muleContext);
        AnnotatedComponent component = new AnnotatedComponent();
        MuleEventContext context = getTestEventContext(TEST_PAYLOAD);
        context.getMessage().setProperty("name", "Ross");
        InvocationResult result = resolver.invoke(component, context);
        assertEquals(result.getState(), InvocationResult.STATE_INVOKED_SUCESSFUL);
        Document doc = DocumentHelper.parseText(TEST_PAYLOAD);
        assertEquals("Hello:Ross:" + doc.asXML(), result.getResult());
    }

    public void testNonAnnotatedMethod() throws Exception
    {
        AnnotatedEntryPointResolver resolver = new AnnotatedEntryPointResolver();
        resolver.setMuleContext(muleContext);
        InvocationResult result = resolver.invoke(new EchoComponent(), getTestEventContext("blah"));
        assertEquals(result.getState(), InvocationResult.STATE_INVOKE_NOT_SUPPORTED);
    }

}

