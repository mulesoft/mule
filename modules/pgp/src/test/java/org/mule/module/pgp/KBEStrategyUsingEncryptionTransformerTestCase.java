/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.pgp;

import org.mule.DefaultMuleEvent;
import org.mule.RequestContext;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transformer.encryption.EncryptionTransformer;
import org.mule.transformer.simple.ByteArrayToObject;


public class KBEStrategyUsingEncryptionTransformerTestCase extends AbstractEncryptionStrategyTestCase
{
    public void testEncrypt() throws Exception
    {
        String msg = "Test Message";
        
        DefaultMuleEvent event = (DefaultMuleEvent)getTestEvent(msg, getTestService("orange", Orange.class));
        RequestContext.setEvent(event);
        
        EncryptionTransformer etrans = new EncryptionTransformer();
        etrans.setStrategy(kbStrategy);
        Object result = etrans.doTransform(msg.getBytes(), "UTF-8");
        
        assertNotNull(result);
        String encrypted = (String) new ByteArrayToObject().doTransform(result,"UTF-8");
        assertTrue(encrypted.startsWith("-----BEGIN PGP MESSAGE-----"));
    }
}