/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.annotations.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.config.transformer.AnnotatedTransformerProxy;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class MimeTypeAwareTransformerTestCase extends AbstractMuleContextTestCase
{

    @Override
    protected void doSetUp() throws Exception
    {
        muleContext.getRegistry().registerObject("mimeTypeAwareTransformer", new MimeTypeAwareTransformer());
    }

    @Test
    public void testTransformerRegistration() throws Exception
    {
        AnnotatedTransformerProxy trans = (AnnotatedTransformerProxy) muleContext.getRegistry()
                .lookupTransformer(MimeTypeAwareTransformer.class.getSimpleName() + ".xmlToJson");

        assertNotNull(trans);
        assertEquals(MimeTypeAwareTransformer.class.getSimpleName() + ".xmlToJson", trans.getName());
        assertEquals("text/xml", trans.getSourceDataTypes().get(0).getMimeType());
        assertEquals("application/json", trans.getReturnDataType().getMimeType());
        assertEquals(1, trans.getSourceDataTypes().size());
        assertEquals(String.class, trans.getSourceDataTypes().get(0).getType());
        assertEquals(5, trans.getPriorityWeighting());
    }

}
