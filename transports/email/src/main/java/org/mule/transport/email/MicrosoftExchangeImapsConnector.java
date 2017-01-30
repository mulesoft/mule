/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email;

import org.mule.api.MuleContext;

import java.util.Properties;

import javax.mail.URLName;

/**
 * Defines a set of properties required by JavaMail in order to use IMAPS with
 * Microsoft Exchange.
 *
 * @since 3.8.0
 */
public class MicrosoftExchangeImapsConnector extends ImapsConnector
{

    public MicrosoftExchangeImapsConnector(MuleContext context)
    {
        super(context);
    }

    @Override
    protected void extendPropertiesForSession(Properties global, Properties local, URLName url)
    {
        super.extendPropertiesForSession(global, local, url);
        addExchangeAuthenticationProperties(ImapsConnector.IMAPS, local);
    }

    /**
     * Disables the following authentication methods in order to use LOGIN as this is the one that
     * works with MS Exchange through IMAPS.
     * The support IMAP username format are:
     * <ul>
     *     <li>using email: jdoe@contoso.com</li>
     *     <li>using Acitve Directory name (note that the name used is the one defined as 'User SamAccountName logon' in AD): CONTOSO\jdoe</li>
     *     <li>accessing a shared mailbox (username same as previous case): CONTOSO\jdoe\sharedmailbox</li>
     * </ul>
     *
     * @param protocol
     * @param properties
     */
    private void addExchangeAuthenticationProperties(String protocol, Properties properties)
    {
        properties.put(String.format("mail.%s.auth.plain.disable",
                                     protocol), "true");
        properties.put(String.format("mail.%s.auth.ntlm.disable",
                                     protocol), "true");
        properties.put(String.format("mail.%s.auth.gssapi.disable",
                                     protocol), "true");

    }
}
