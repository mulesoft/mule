/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.support;

import org.apache.cxf.interceptor.StaxInInterceptor;
import org.apache.cxf.phase.Phase;

/**
 * Resets the ReversibleXMLStreamReader so the person receiving it can start back
 * at the beginning of the stream.
 */
public class ReversibleStaxInInterceptor extends ReversibleStaxInterceptor
{

    public ReversibleStaxInInterceptor()
    {
        super(Phase.POST_STREAM);
        getAfter().add(StreamClosingInterceptor.class.getName());
        getAfter().add(StaxInInterceptor.class.getName());
    }

}


