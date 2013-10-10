/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.email.issues;

import org.mule.transport.email.functional.AbstractEmailFunctionalTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class Pop3MimeWithConnectorMule2042TestCase extends AbstractEmailFunctionalTestCase
{

    public Pop3MimeWithConnectorMule2042TestCase(ConfigVariant variant, String configResources)
    {
        super(variant, MIME_MESSAGE, "pop3", configResources);
    }
    
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "pop3-mime-with-connector-mule-2042-test-service.xml"},
            {ConfigVariant.FLOW, "pop3-mime-with-connector-mule-2042-test-flow.xml"}
        });
    }      
    
    @Test
    public void testRequest() throws Exception
    {
        doRequest();
    }

}
