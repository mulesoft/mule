/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

    MuleMessage request(String url, long timeout) throws MuleException;

    MuleManagedConnection getManagedConnection();

    void close() throws ResourceException;

    void associateConnection(MuleManagedConnection newMc) throws ResourceException;

    MuleMessage send(String url, Object payload, Map messageProperties) throws MuleException;
}
