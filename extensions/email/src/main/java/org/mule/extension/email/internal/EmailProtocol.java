/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal;

import static java.lang.String.format;
import org.mule.extension.email.api.EmailConnector;

import java.net.Socket;

import javax.net.SocketFactory;

/**
 *  Email Protocols supported by the {@link EmailConnector}.
 *
 *  Each protocol have a set of properties that need to be configured
 *  to establish a connection with a mail server.
 *
 * @since 4.0
 */
public enum EmailProtocol
{

    /**
     * represents the Simple Mail Transfer Protocol.
     */
    SMTP("smtp", false),

    /**
     * represents the secured Simple Mail Transfer Protocol.
     */
    SMTPS("smtp", true),

    /**
     * represents the Internet Message Access Protocol.
     */
    IMAP("imap", false),

    /**
     * represents the secured Internet Message Access Protocol.
     */
    IMAPS("imap", true),

    /**
     * represents the Post Office Protocol.
     */
    POP3("pop3", false),

    /**
     * represents the secured Post Office Protocol.
     */
    POP3S("pop3", true);

    /**
     * the Default port for the SMTP protocol
     */
    public static final String SMTP_PORT = "25";


    /**
     * the Default port for the SMTPS protocol
     */
    public static final String SMTPS_PORT = "465";


    /**
     * the Default port for the POP3 protocol
     */
    public static final String POP3_PORT = "110";


    /**
     * the Default port for the POP3S protocol
     */
    public static final String POP3S_PORT = "995";


    /**
     * the Default port for the IMAP protocol
     */
    public static final String IMAP_PORT = "143";

    /**
     * the Default port for the IMAPS protocol
     */
    public static final String IMAPS_PORT = "993";

    /**
     * The host name of the mail server.
     */
    private static final String HOST_NAME_PROPERTY_MASK = "mail.%s.host";

    /**
     * The port number of the mail server.
     */
    private static final String PORT_PROPERTY_MASK = "mail.%s.port";

    /**
     * Socket connection timeout value in milliseconds. Default is infinite timeout.
     */
    private static final String CONNECTION_TIMEOUT_PROPERTY_MASK = "mail.%s.connectiontimeout";

    /**
     * Socket write timeout value in milliseconds. Default is infinite timeout.
     */
    private static final String WRITE_TIMEOUT_PROPERTY_MASK = "mail.%s.writetimeout";

    /**
     * Indicates if should attempt to authorize or not. Defaults to false.
     */
    private static final String MAIL_AUTH_PROPERTY_MASK = "mail.%s.auth";

    /**
     * Indicates if the STARTTLS command shall be used to initiate a TLS-secured connection.
     */
    private static final String START_TLS_PROPERTY_MASK = "mail.%s.starttls.enable";

    /**
     * Specifies the {@link SocketFactory} class to create smtp sockets.
     */
    private static final String SOCKET_FACTORY_PROPERTY_MASK = "mail.%s.ssl.socketFactory";

    /**
     * Whether to use {@link Socket} as a fallback if the initial connection fails or not.
     */
    private static final String SOCKET_FACTORY_FALLBACK_PROPERTY_MASK = "mail.%s.socketFactory.fallback";

    /**
     * Specifies the SSL cipher suites that will be enabled for SSL connections.
     */
    private static final String SSL_CIPHERSUITES_MASK = "mail.%s.ssl.ciphersuites";

    /**
     * Specifies the SSL protocols that will be enabled for SSL connections.
     */
    private static final String SSL_PROTOCOLS_MASK = "mail.%s.ssl.protocols";

    /**
     * Specifies the trusted hosts.
     */
    private static final String SSL_TRUST_MASK = "mail.%s.ssl.trust";

    /**
     * Specifies if ssl is enabled or not.
     */
    private static final String SSL_ENABLE_MASK = "mail.%s.ssl.enable";

    /**
     * Specifies the port to connect to when using a socket factory.
     */
    private static final String SOCKET_FACTORY_PORT_MASK = "mail.%s.socketFactory.port";

    /**
     * Specifies the default transport name.
     */
    private static final String TRANSPORT_NAME_PROTOCOL = "mail.transport.name";

    /**
     * Socket read timeout value in milliseconds. This timeout is implemented by {@link Socket}. Default is infinite timeout.
     */
    private static final String READ_TIMEOUT_PROPERTY_MASK = "mail.%s.timeout";

    /**
     * Defines the default mime charset to use when none has been specified for the message.
     */
    private static final String MAIL_MIME_CHARSET = "mail.mime.charset";

    private final String name;
    private final boolean secure;

    /**
     * Creates an instance.
     *
     * @param name the name of the protocol.
     */
    EmailProtocol(String name, boolean secure)
    {
        this.name = name;
        this.secure = secure;
    }

    /**
     * @return the name of the protocol.
     */
    public String getName()
    {
        return name;
    }

    /**
     * @return whether a protocol is secured by SSL/TLS or not.
     */
    public boolean isSecure()
    {
        return secure;
    }

    /**
     * @return the protocol host name property.
     */
    public String getHostProperty()
    {
        return unmaskProperty(HOST_NAME_PROPERTY_MASK);
    }

    /**
     * @return the protocol mail auth property.
     */
    public String getMailAuthProperty()
    {
        return unmaskProperty(MAIL_AUTH_PROPERTY_MASK);
    }

    /**
     * @return the mime charset property.
     */
    public String getMailMimeCharsetProperty()
    {
        return unmaskProperty(MAIL_MIME_CHARSET);
    }

    /**
     * @return the protocol port property.
     */
    public String getPortProperty()
    {
        return unmaskProperty(PORT_PROPERTY_MASK);
    }

    /**
     * @return the protocol socket factory fallback property.
     */
    public String getSocketFactoryFallbackProperty()
    {
        return unmaskProperty(SOCKET_FACTORY_FALLBACK_PROPERTY_MASK);
    }

    /**
     * @return the protocol socket factory port property.
     */
    public String getSocketFactoryPortProperty()
    {
        return unmaskProperty(SOCKET_FACTORY_PORT_MASK);
    }

    /**
     * @return the protocol socket factory property.
     */
    public String getSocketFactoryProperty()
    {
        return unmaskProperty(SOCKET_FACTORY_PROPERTY_MASK);
    }

    /**
     * @return the protocol ssl ciphersuites property.
     */
    public String getSslCiphersuitesProperty()
    {
        return unmaskProperty(SSL_CIPHERSUITES_MASK);
    }

    /**
     * @return the protocol ssl enabled protocols property.
     */
    public String getSslProtocolsProperty()
    {
        return unmaskProperty(SSL_PROTOCOLS_MASK);
    }

    /**
     * @return the ssl enable property.
     */
    public String getSslEnableProperty()
    {
        return unmaskProperty(SSL_ENABLE_MASK);
    }

    /**
     * @return the protocol ssl trust property.
     */
    public String getSslTrustProperty()
    {
        return unmaskProperty(SSL_TRUST_MASK);
    }

    /**
     * @return the protocol start tls property.
     */
    public String getStartTlsProperty()
    {
        return unmaskProperty(START_TLS_PROPERTY_MASK);
    }

    /**
     * @return the protocol name property.
     */
    public String getTransportProtocolProperty()
    {
        return TRANSPORT_NAME_PROTOCOL;
    }

    /**
     * @return the protocol read timeout property.
     */
    public String getReadTimeoutProperty()
    {
        return unmaskProperty(READ_TIMEOUT_PROPERTY_MASK);
    }

    /**
     * @return the protocol connection timeout property.
     */
    public String getConnectionTimeoutProperty()
    {
        return unmaskProperty(CONNECTION_TIMEOUT_PROPERTY_MASK);
    }

    /**
     * @return the protocol write timeout property.
     */
    public String getWriteTimeoutProperty()
    {
        return unmaskProperty(WRITE_TIMEOUT_PROPERTY_MASK);
    }

    private String unmaskProperty(String property)
    {
        return format(property, name);
    }
}
