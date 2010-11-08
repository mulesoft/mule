/**
 * Created by IntelliJ IDEA.
 * User: mike.schilling
 * Date: Oct 15, 2010
 * Time: 3:00:34 PM
 * To change this template use File | Settings | File Templates.
 */

package org.mule.transport.email.transformers;

import org.mule.transport.email.functional.AbstractEmailFunctionalTestCase;

import javax.activation.MimeType;
import javax.mail.BodyPart;
import javax.mail.internet.MimeMultipart;
import java.util.Arrays;
import java.util.List;

public class SmtpAttachmentsFunctionalOutboundAttachmentsOnlyTestCase extends AbstractEmailFunctionalTestCase
{

    public SmtpAttachmentsFunctionalOutboundAttachmentsOnlyTestCase()
    {
        super(STRING_MESSAGE, "smtp", "smtp-functional-test-outbound-attachments-only.xml");
        setAddAttachments(true);
    }

    public void testSend() throws Exception
    {
        doSend();
    }

    @Override
    protected void verifyMessage(MimeMultipart content) throws Exception
    {
        assertEquals(3, content.getCount());
        verifyMessage((String) content.getBodyPart(0).getContent());
        List expectedTypes = Arrays.asList("application/text", "application/xml");
        for (int i = 1; i < 2; i++)
        {
            BodyPart part = content.getBodyPart(i);
            String type = part.getContentType();
            MimeType mt = new MimeType(type);
            assertTrue(expectedTypes.contains(mt.getPrimaryType() + "/" + mt.getSubType()));
        }
    }
}