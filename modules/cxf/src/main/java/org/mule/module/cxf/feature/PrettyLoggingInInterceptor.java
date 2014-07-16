/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.feature;

import java.io.PrintWriter;

import org.apache.cxf.interceptor.LoggingInInterceptor;

/**
 * Takes the log output from the standard CXF LoggingInterceptor, 
 * disassembles it, pretty-prints the XML payload, then puts it all back 
 * together again.
 */
public class PrettyLoggingInInterceptor extends LoggingInInterceptor
{
    public PrettyLoggingInInterceptor()
    {
        super();
    }

    public PrettyLoggingInInterceptor(String phase)
    {
        super(phase);
    }

    public PrettyLoggingInInterceptor(int lim)
    {
        super(lim);
    }

    public PrettyLoggingInInterceptor(PrintWriter w)
    {
        super(w);
    }

    @Override
    protected String transform(String originalLogString)
    {
        return PrettyLoggingFeature.formatXmlPayload(originalLogString);
    }
}
