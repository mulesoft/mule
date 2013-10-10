/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.feature;

import java.io.PrintWriter;

import org.apache.cxf.interceptor.LoggingOutInterceptor;

/**
 * Takes the log output from the standard CXF LoggingInterceptor, 
 * disassembles it, pretty-prints the XML payload, then puts it all back 
 * together again.
 */
public class PrettyLoggingOutInterceptor extends LoggingOutInterceptor
{
    public PrettyLoggingOutInterceptor()
    {
        super();
    }

    public PrettyLoggingOutInterceptor(String phase)
    {
        super(phase);
    }

    public PrettyLoggingOutInterceptor(int lim)
    {
        super(lim);
    }

    public PrettyLoggingOutInterceptor(PrintWriter w)
    {
        super(w);
    }

    @Override
    protected String transform(String originalLogString)
    {
        return PrettyLoggingFeature.formatXmlPayload(originalLogString);
    }
}
