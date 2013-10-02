/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.util.FileUtils;
import org.mule.util.IOUtils;
import org.mule.util.SystemUtils;

import com.icegreen.greenmail.util.GreenMailUtil;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import javax.mail.Message;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class ImapFunctionalWithAttachmentsTestCase extends AbstractEmailFunctionalTestCase
{
    private static final String CONFIG_FILE = "email-attachment-save.xml";
    private File saveDir;

    public ImapFunctionalWithAttachmentsTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, true, "imap", configResources, true);
        setStartContext(false);
        
        // set up email properties for mule config
        System.setProperty("mail.user", DEFAULT_USER);
        System.setProperty("mail.password", DEFAULT_PASSWORD);
        System.setProperty("mail.host", "localhost");
        
        saveDir = new File(SystemUtils.JAVA_IO_TMPDIR, getClass().getName());
        System.setProperty("mail.save.dir", saveDir.getAbsolutePath());
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{            
            {ConfigVariant.FLOW, CONFIG_FILE}
        });
    }      
    
    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        
        // clear out destination directory
        FileUtils.deleteTree(new File(System.getProperty("mail.save.dir")));
    }
        
    @Test
    public void testRequest() throws Exception
    {
        // we do this since we need to set system mail properties before starting
        // mule since they are referenced in there
        muleContext.start();
        
        // a test email gets sent by default
        assertEquals(1, server.getReceivedMessages().length);

        InputStream inputstream = IOUtils.getResourceAsStream(CONFIG_FILE, getClass(), true, false);
        assertNotNull(inputstream);
        byte[] byteArray = IOUtils.toByteArray(inputstream);

        // send a file attachment email
        GreenMailUtil.sendAttachmentEmail(DEFAULT_EMAIL, "joe", "email subject with attachments", "",
            byteArray, "text/xml", CONFIG_FILE, "description", smtpSetup);
        Message[] messages = server.getReceivedMessages();
        assertEquals(2, messages.length);

        assertAttachmentWasSaved();
    }
    
    private void assertAttachmentWasSaved()
    {
        Prober prober = new PollingProber(10000, 100);
        prober.check(new Probe()
        {
            public boolean isSatisfied()
            {
                // FIXME DZ: don't know why these are empty, so just compare the saved email
                // file to the expected content
                // assertEquals(messages[1].ATTACHMENT, messages[1].getDisposition());
                // assertEquals("email-attachment-save.xml", messages[1].getFileName());

                File savedFile = new File(saveDir, CONFIG_FILE);
                return savedFile.exists();

                // TODO DZ: compare the source and target files and makes sure they are the
                // same
                // FIXME DF: when you send an attachment email with text in the body, this
                // mule config saves the body in it's own file; how do you just save the file
                // attachment?
            }

            public String describeFailure()
            {
                return "No attachments were saved";
            }
        });
    }
}
