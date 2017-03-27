/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.functional;

import static java.util.Arrays.asList;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import java.util.Collection;

public class ImapSpecialCharactersTestCase extends AbstractEmailFunctionalTestCase
{

    public static final String SPECIAL_CHARACTER_USER = "%21%23bob%23%25";
    public static final String SPECIAL_CHARACTER_PASSWORD = "*uawH*IDXlh2p%21xSPOx%23%25zLpL";

    public ImapSpecialCharactersTestCase(ConfigVariant variant, String configResources, String user, String password)
    {
        super(variant, STRING_MESSAGE, "imap", configResources, DEFAULT_EMAIL, user, DEFAULT_MESSAGE, password, null);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return asList(new Object[][]{
            {ConfigVariant.FLOW, "imap-special-characters-password-test-flow.xml", DEFAULT_USER, SPECIAL_CHARACTER_PASSWORD},
            {ConfigVariant.FLOW, "imap-special-characters-user-test-flow.xml", SPECIAL_CHARACTER_USER, DEFAULT_PASSWORD},
        });
    }

    @Test
    public void requestWithSpecialCharacters() throws Exception
    {
        doRequest();
    }

}
