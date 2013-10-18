/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
