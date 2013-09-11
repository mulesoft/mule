/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis.extensions;

import org.mule.api.DefaultMuleException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.soap.axis.AxisConnector;

import java.util.HashMap;
import java.util.Map;

import org.apache.axis.client.Transport;

/**
 * A container for all Mule supported transports for Axis.
 */
public class MuleTransport extends Transport
{

    private static Map<String, Class> transports = null;

    public MuleTransport()
    {
        transportName = "MuleTransport";
    }

    public MuleTransport(String protocol)
    {
        transportName = protocol;
    }

    private static void initTransports()
    {
        transports = new HashMap<String, Class>();
        transports.put("http", HTTP.class);
        transports.put("https", HTTPS.class);
        transports.put("servlet", SERVLET.class);
        transports.put("tcp", TCP.class);
        transports.put("ssl", SSL.class);
        transports.put("jms", JMS.class);
        transports.put("vm", VM.class);
        transports.put("xmpp", XMPP.class);
        transports.put("smtp", SMTP.class);
        transports.put("smtps", SMTPS.class);
        transports.put("pop3", POP3.class);
        transports.put("pop3s", POP3S.class);
        transports.put("imap", IMAP.class);
        transports.put("imaps", IMAPS.class);
    }

    /**
     * @param protocol the Axis soap transport to use
     * @return The corresponding transport class
     * @throws DefaultMuleException if the transport is not supported by Axis
     * @throws NullPointerException if the transport protocol is null
     */
    public static Class getTransportClass(String protocol) throws DefaultMuleException
    {
        if (protocol == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("protocol").toString());
        }
        if (!isTransportSupported(protocol))
        {
            throw new DefaultMuleException(
                CoreMessages.schemeNotCompatibleWithConnector(protocol, AxisConnector.class));
        }
        return transports.get(protocol);
    }

    public static boolean isTransportSupported(String protocol)
    {
        if (transports == null)
        {
            initTransports();
        }
        return transports.get(protocol) != null;
    }

    public static class HTTP extends MuleTransport
    {
        public HTTP()
        {
            super("http");
        }
    }

    public static class HTTPS extends MuleTransport
    {
        public HTTPS()
        {
            super("https");
        }
    }

    public static class TCP extends MuleTransport
    {
        public TCP()
        {
            super("tcp");
        }
    }

    public static class SSL extends MuleTransport
    {
        public SSL()
        {
            super("ssl");
        }
    }

    public static class JMS extends MuleTransport
    {
        public JMS()
        {
            super("jms");
        }
    }

    public static class POP3 extends MuleTransport
    {
        public POP3()
        {
            super("pop3");
        }
    }

    public static class SMTP extends MuleTransport
    {
        public SMTP()
        {
            super("smtp");
        }
    }

    public static class POP3S extends MuleTransport
    {
        public POP3S()
        {
            super("pop3s");
        }
    }

    public static class SMTPS extends MuleTransport
    {
        public SMTPS()
        {
            super("smtps");
        }
    }

    public static class IMAP extends MuleTransport
    {
        public IMAP()
        {
            super("imap");
        }
    }

    public static class IMAPS extends MuleTransport
    {
        public IMAPS()
        {
            super("imaps");
        }
    }

    public static class XMPP extends MuleTransport
    {
        public XMPP()
        {
            super("xmpp");
        }
    }

    public static class VM extends MuleTransport
    {
        public VM()
        {
            super("vm");
        }
    }

    public static class SERVLET extends MuleTransport
    {
        public SERVLET()
        {
            super("servlet");
        }
    }
}
