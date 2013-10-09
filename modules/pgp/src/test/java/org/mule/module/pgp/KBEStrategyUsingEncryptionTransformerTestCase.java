/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.pgp;

import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transformer.encryption.EncryptionTransformer;
import org.mule.transformer.simple.ByteArrayToObject;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class KBEStrategyUsingEncryptionTransformerTestCase extends AbstractEncryptionStrategyTestCase
{
    @Test
    public void testEncrypt() throws Exception
    {
        String msg = "Test Message";

        MuleEvent event = getTestEvent(msg, getTestService("orange", Orange.class));
        event = RequestContext.setEvent(event);

        EncryptionTransformer etrans = new EncryptionTransformer();
        etrans.setStrategy(kbStrategy);
        Object result = etrans.doTransform(msg.getBytes(), "UTF-8");

        assertNotNull(result);
        InputStream inputStream = (InputStream) result;
        String message = IOUtils.toString(inputStream);
        String encrypted = (String) new ByteArrayToObject().doTransform(message.getBytes(), "UTF-8");
        assertTrue(encrypted.startsWith("-----BEGIN PGP MESSAGE-----"));
    }
}
