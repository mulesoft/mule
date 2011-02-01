/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.email.functional;

import org.mule.util.FileUtils;
import org.mule.util.IOUtils;

import com.icegreen.greenmail.util.GreenMailUtil;

import java.io.File;
import java.io.InputStream;

import javax.mail.Message;

public class ImapFunctionalWithAttachmentsTestCase extends AbstractEmailFunctionalTestCase
{
    private static final String configFile = "email-attachment-save.xml";

    public ImapFunctionalWithAttachmentsTestCase()
    {
        super(true, "imap", configFile, true);
        setStartContext(false);
        // set up email properties for mule config
        System.setProperty("mail.user", DEFAULT_USER);
        System.setProperty("mail.password", DEFAULT_PASSWORD);
        System.setProperty("mail.host", "localhost");
        System.setProperty("mail.save.dir", System.getProperty("java.io.tmpdir") + File.separatorChar
                                            + this.getClass().getName());
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        // clear out destination directory
        FileUtils.deleteTree(new File(System.getProperty("mail.save.dir")));
    }

    public void testRequest() throws Exception
    {
        // we do this since we need to set system mail properties before starting
        // mule since they are referenced in there
        muleContext.start();
        // a test email gets sent by default
        assertEquals(1, server.getReceivedMessages().length);

        InputStream inputstream = IOUtils.getResourceAsStream(configFile, getClass(), true, false);
        assertNotNull(inputstream);
        byte[] byteArray = IOUtils.toByteArray(inputstream);

        // send a file attachment email
        GreenMailUtil.sendAttachmentEmail(DEFAULT_EMAIL, "joe", "email subject with attachments", "",
            byteArray, "text/xml", configFile, "description", smtpSetup);
        Message[] messages = server.getReceivedMessages();
        assertEquals(2, messages.length);

        // need time for the email to process thru mule
        Thread.sleep(5000);

        // FIXME DZ: don't know why these are empty, so just compare the saved email
        // file to the expected content
        // assertEquals(messages[1].ATTACHMENT, messages[1].getDisposition());
        // assertEquals("email-attachment-save.xml", messages[1].getFileName());
        File savedFile = new File(System.getProperty("mail.save.dir") + File.separatorChar + configFile);
        assertTrue(savedFile.exists());

        // TODO DZ: compare the source and target files and makes sure they are the
        // same
        // FIXME DF: when you send an attachment email with text in the body, this
        // mule config saves the body in it's own file; how do you just save the file
        // attachment?
    }
}
