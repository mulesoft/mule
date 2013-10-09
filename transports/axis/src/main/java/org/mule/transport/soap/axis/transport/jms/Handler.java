/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.soap.axis.transport.jms;

import org.mule.transport.soap.axis.transport.VoidURLConnection;

import java.net.URL;
import java.net.URLConnection;

/**
 * A Dummy Url handler for handling jms. This is needed becuase Axis uses
 * urlStreamHandlers to parse non-http urls.
 */
public class Handler extends java.net.URLStreamHandler
{
    protected URLConnection openConnection(URL url)
    {
        return new VoidURLConnection(url);
    }
}
