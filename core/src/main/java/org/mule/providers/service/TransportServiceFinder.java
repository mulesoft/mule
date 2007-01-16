/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.service;

/**
 * <code>TransportServiceFinder</code> can be used as a hook into the connector
 * service creation process to return the correct Service Descriptor for a given
 * service name. By default the service name is looked up directly, however a generic
 * service name might be used where the real service implementation will be used i.e.
 * in the case of a soap connector the finder could check the classpath for Axis or
 * Glue and return the correct descriptor.
 * 
 */
public interface TransportServiceFinder
{
    TransportServiceDescriptor findService(String service, TransportServiceDescriptor csd)
        throws TransportFactoryException;
}
