/*
 * $Id: CxfMessageRequesterFactory.java 9935 2007-11-28 18:47:01Z acooke $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.cxf;

import org.mule.providers.AbstractMessageRequesterFactory;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOMessageRequester;

/**
 * <code>CxfMessageRequesterFactory</code> creates an CxfMessageRequester, used
 * for making SOAP calls using the CXF framework.
 */
public class CxfMessageRequesterFactory extends AbstractMessageRequesterFactory
{
    public UMOMessageRequester create(UMOImmutableEndpoint endpoint) throws UMOException
    {
        return new CxfMessageRequester(endpoint);
    }
}