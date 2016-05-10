/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.util;

/**
 * Constants used by the email connector classes.
 *
 * @since 4.0
 */
public final class EmailConstants
{

    /**
     * charset constant for koi8-r
     */
    public static final String KOI8_R = "koi8-r";

    /**
     * charset constant for iso-8859-1
     */
    public static final String ISO_8859_1 = "iso-8859-1";

    /**
     * charset constant for us-ascii
     */
    public static final String US_ASCII = "us-ascii";

    /**
     * charset constant for utf-8
     */
    public static final String UTF_8 = "utf-8";

    /**
     * defines all the multipart content types
     */
    public static final String MULTIPART = "multipart/*";

    /**
     * defines all the text content types
     */
    public static final String TEXT = "text/*";

    /**
     * the SMTP protocol key
     */
    public static final String PROTOCOL_SMTP = "smtp";

    /**
     * the SMTPS protocol key
     */
    public static final String PROTOCOL_SMTPS = "smtps";

    /**
     * the POP3 protocol key
     */
    public static final String PROTOCOL_POP3 = "pop3";

    /**
     * the POP3S protocol key
     */
    public static final String PROTOCOL_POP3S = "pop3s";

    /**
     * the IMAP protocol key
     */
    public static final String PROTOCOL_IMAP = "imap";

    /**
     * the IMAPS protocol key
     */
    public static final String PROTOCOL_IMAPS = "imaps";

    /**
     * the Default port for the SMTP protocol
     */
    public static final String PORT_SMTP = "25";


    /**
     * the Default port for the SMTPS protocol
     */
    public static final String PORT_SMTPS = "465";


    /**
     * the Default port for the POP3 protocol
     */
    public static final String PORT_POP3 = "110";


    /**
     * the Default port for the POP3S protocol
     */
    public static final String PORT_POP3S = "995";


    /**
     * the Default port for the IMAP protocol
     */
    public static final String PORT_IMAP = "143";

    /**
     * the Default port for the IMAPS protocol
     */
    public static final String PORT_IMAPS = "993";

    /**
     * The default mailbox name.
     */
    public static final String DEFAULT_FOLDER = "INBOX";

    /**
     * Hide constructor
     */
    private EmailConstants()
    {

    }

}
