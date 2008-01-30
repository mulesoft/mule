/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transport.MessageRequester;
import org.mule.transport.AbstractMessageRequesterFactory;

/**
 * <code>CxfMessageRequesterFactory</code> creates an CxfMessageRequester, used
 * for making SOAP calls using the CXF framework.
 */
public class CxfMessageRequesterFactory extends AbstractMessageRequesterFactory
{
    public MessageRequester create(ImmutableEndpoint endpoint) throws MuleException
    {
        return new CxfMessageRequester(endpoint);
    }
}