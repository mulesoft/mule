/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.jca;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;

import java.util.Map;

import javax.resource.ResourceException;

/**
 * <code>MuleConnection</code> defines the client connection methods for the JCA
 * CCI contract
 */
public interface MuleConnection
{
    void dispatch(String url, Object payload, Map messageProperties) throws MuleException;

    MuleMessage receive(String url, long timeout) throws MuleException;

    MuleManagedConnection getManagedConnection();

    void close() throws ResourceException;

    void associateConnection(MuleManagedConnection newMc) throws ResourceException;

    MuleMessage send(String url, Object payload, Map messageProperties) throws MuleException;
}
