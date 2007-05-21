/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.security;

/**
 * Set the underlying protocol handler.  As far as I know, this is untested.
 */
public interface TlsProtocolHandler
{

    String getProtocolHandler();

    void setProtocolHandler(String protocolHandler);

}
