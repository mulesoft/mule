/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.construct.builder;

import org.mule.component.AbstractJavaComponent;
import org.mule.component.simple.EchoComponent;
import org.mule.construct.SimpleService;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transformer.simple.StringAppendTransformer;

import java.util.Collections;

public class SimpleServiceBuilderTestCase extends AbstractMuleTestCase
{
    public void testFullConfiguration() throws Exception
    {
        SimpleService simpleService = new SimpleServiceBuilder().name("test-simple-service")
            .inboundAddress("test://foo")
            .inboundTransformers(Collections.singleton(new StringAppendTransformer("bar")))
            .component(EchoComponent.class)
            .build(muleContext);

        assertEquals("test-simple-service", simpleService.getName());
        assertEquals(EchoComponent.class,
            ((AbstractJavaComponent) simpleService.getComponent()).getObjectType());
    }
}
