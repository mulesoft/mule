/*
 * $Id: ExpressionTransformerTestCase.java 18265 2010-07-19 09:42:45Z rossmason $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.simple;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.Transformer;
import org.mule.tck.FunctionalTestCase;

import java.util.Arrays;
import java.util.List;

public class TransformationContentTypeTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "content-type-setting-transformer-configs.xml";
    }

    public void testReturnType() throws Exception
    {
        Transformer trans = muleContext.getRegistry().lookupTransformer("testTransformer");
        assertNotNull(trans);
        String inputMessage = "ABCDEF";
        byte[] array = (byte[]) trans.transform(inputMessage);

        MuleMessage message = new DefaultMuleMessage(inputMessage, muleContext);
        List<Transformer> transformers = Arrays.asList(new Transformer[] {trans});
        message.applyTransformers(null, transformers);
        assertEquals("text/plain", message.getDataType().getMimeType());
        assertEquals("iso-8859-1", message.getEncoding());

    }
}
