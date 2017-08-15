/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.pgp;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.module.pgp.i18n.PGPMessages.noFileKeyFound;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.module.pgp.exception.MissingPGPKeyException;

import org.junit.Test;

public class PGPKeyRingValidationsTestCase
{
    private PGPKeyRingImpl pgpKeyRing = new PGPKeyRingImpl();

    @Test
    public void testInvalidPublicKeyRingFileName()
    {
        pgpKeyRing.setPublicKeyRingFileName("incorrectPath");
        try
        {
            pgpKeyRing.initialise();
            fail("InitialisationException should be triggered because public key file doesn't exist");
        }
        catch (InitialisationException initialisationException)
        {
            assertThat(initialisationException.getMessage(), is(noFileKeyFound("incorrectPath").getMessage()));
        }
    }

    @Test
    public void testInvalidSecretKeyRingFileName() throws Exception
    {
        pgpKeyRing.setSecretKeyRingFileName("incorrectPath");
        try
        {
            pgpKeyRing.getConfiguredSecretKey();
            fail("MissingPGPKeyException should be triggered because Secret Key File doesn't exist");
        }
        catch (MissingPGPKeyException missingPGPKeyException)
        {
            assertThat(missingPGPKeyException.getMessage(), is(noFileKeyFound("incorrectPath").getMessage()));
        }
    }

}
