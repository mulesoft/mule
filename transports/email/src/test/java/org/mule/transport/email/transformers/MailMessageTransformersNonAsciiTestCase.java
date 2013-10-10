/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.email.transformers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.Transformer;
import org.mule.config.i18n.LocaleMessageHandler;
import org.mule.transport.email.MailProperties;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.mail.internet.MimeMessage;

import org.junit.Test;

public class MailMessageTransformersNonAsciiTestCase extends MailMessageTransformersTestCase
{

    @Override
    protected String getContentType() 
    {
        return "text/plain; charset=iso-2022-jp";
    }
    
    @Override
    public Object getResultData()
    {
        return LocaleMessageHandler.getString("test-data", Locale.JAPAN,
            "MailMessageTransformersNonAsciiTestCase.getResultData", new Object[] {});
    }

    @Test
    public void testNonAsciiSubjectEncodingInRoundtripTransformation() throws Exception
    {
        Transformer roundTripTransformer = super.getRoundTripTransformer();

        // Build message setting the non-ascii message subject as a property for the transformer.
        Map<String, Object> outboundProperties = new HashMap<String, Object>();
        String testSubject = (String) getResultData();
        outboundProperties.put(MailProperties.SUBJECT_PROPERTY, testSubject);
        MuleMessage message = new DefaultMuleMessage(testSubject, outboundProperties, muleContext);

        // Hack to set the default charset used by the javax.mail API to perform the mail subject encoding as iso-8859-1.
        System.setProperty("mail.mime.charset", "iso-8859-1");

        // Transform.
        Object result = roundTripTransformer.transform(message, "iso-2022-jp");

        assertNotNull("The result of the roundtrip transform shouldn't be null", result);
        assertTrue(result instanceof MimeMessage);
        // Assert that the mail subject has been correctly encoded in iso-2022-jp.
        assertEquals(((MimeMessage) result).getSubject(), testSubject);
    }
}
