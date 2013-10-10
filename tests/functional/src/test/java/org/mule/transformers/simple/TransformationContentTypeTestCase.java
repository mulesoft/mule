/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformers.simple;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.Transformer;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TransformationContentTypeTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "content-type-setting-transformer-configs.xml";
    }

    @Test
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
