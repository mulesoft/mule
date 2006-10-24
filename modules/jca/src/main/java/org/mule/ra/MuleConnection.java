/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.ra;

import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;

import javax.resource.ResourceException;

import java.util.Map;

/**
 * <code>MuleConnection</code> defines the client connection methods for the JCA
 * cci contract
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface MuleConnection
{
    void dispatch(String url, Object payload, Map messageProperties) throws UMOException;

    UMOMessage receive(String url, long timeout) throws UMOException;

    MuleManagedConnection getManagedConnection();

    void close() throws ResourceException;

    void associateConnection(MuleManagedConnection newMc) throws ResourceException;

    UMOMessage send(String url, Object payload, Map messageProperties) throws UMOException;
}
