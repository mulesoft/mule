/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.functional;

import org.junit.Test;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

public class ImapExceptionStrategyTestCase extends AbstractEmailFunctionalTestCase
{
    public ImapExceptionStrategyTestCase(ConfigVariant variant,
                                         String configResources)
    {
        super(variant, STRING_MESSAGE, "imap", configResources);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                { ConfigVariant.FLOW, "imap-exception-strategy.xml" } });
    }

    @Test
    public void usesExceptionStrategy() throws Exception
    {
        doRequest();
    }
}
