/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.functional;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

public class ImapSpecialCharactersTestCase extends AbstractEmailFunctionalTestCase
{

    public ImapSpecialCharactersTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, STRING_MESSAGE, "imap", configResources, DEFAULT_EMAIL, DEFAULT_USER, DEFAULT_MESSAGE, "*uawH*IDXlh2p!xSPOx#%zLpL", null);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
                {ConfigVariant.FLOW, "imap-special-characters-test-flow.xml"}
        });
    }

    @Test
    public void testRequest() throws Exception
    {
        doRequest();
    }

}
