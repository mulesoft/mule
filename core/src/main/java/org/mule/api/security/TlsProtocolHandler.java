/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.security;

/**
 * Set the underlying protocol handler.  As far as I know, this is untested.
 */
public interface TlsProtocolHandler
{

    String getProtocolHandler();

    void setProtocolHandler(String protocolHandler);

}
