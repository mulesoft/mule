/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.pgp;

import org.mule.api.ExceptionPayload;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.config.MuleProperties;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.util.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PGPSecurityFilterTestCase extends AbstractServiceAndFlowTestCase
{
    protected static final String TARGET = "/encrypted.txt";
    protected static final String DIRECTORY = "output";
    protected static final String MESSAGE_EXCEPTION = "Crypto Failure";

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "test-pgp-encrypt-config-service.xml"},
            {ConfigVariant.FLOW, "test-pgp-encrypt-config-flow.xml"}});
    }

    public PGPSecurityFilterTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Override
    protected boolean isDisabledInThisEnvironment()
    {
        return (AbstractEncryptionStrategyTestCase.isCryptographyExtensionInstalled() == false);
    }

    @Test
    public void testAuthenticationAuthorised() throws Exception
    {
        MuleClient client = muleContext.getClient();

        byte[] msg = loadEncryptedMessage();
        Map<String, Object> props = createMessageProperties();

        client.dispatch("vm://echo", new String(msg), props);

        MuleMessage message = client.request("vm://output", RECEIVE_TIMEOUT);
        assertEquals("This is a test message.\r\nThis is another line.\r\n", message.getPayloadAsString());
    }

    @Test
    public void testAuthenticationNotAuthorised() throws Exception
    {
        Map<String, Object> props = createMessageProperties();
        MuleMessage reply = muleContext.getClient().send("vm://echo", "An unsigned message", props);
        assertNotNull(reply.getExceptionPayload());
        ExceptionPayload excPayload = reply.getExceptionPayload();
        assertEquals(MESSAGE_EXCEPTION, excPayload.getMessage());
    }

    private byte[] loadEncryptedMessage() throws IOException
    {
        URL url = Thread.currentThread().getContextClassLoader().getResource("./encrypted-signed.asc");

        FileInputStream in = new FileInputStream(url.getFile());
        byte[] msg = IOUtils.toByteArray(in);
        in.close();

        return msg;
    }

    private Map<String, Object> createMessageProperties()
    {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("TARGET_FILE", TARGET);
        props.put(MuleProperties.MULE_USER_PROPERTY, "Mule server <mule_server@mule.com>");
        return props;
    }
}
