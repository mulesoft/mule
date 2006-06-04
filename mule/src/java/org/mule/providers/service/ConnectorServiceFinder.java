/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.service;

/**
 * <code>ConnectorServiceFinder</code> can be used as a hook into the
 * connector service creation process to return the correct Service Descriptor
 * for a given service name. By default the service name is looked up directly,
 * however a generic service name might be used where the real service
 * implementation will be used i.e. in the case of a soap connector the finder
 * could check the classpath for Axis or Glue and return the correct descriptor.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface ConnectorServiceFinder
{
    ConnectorServiceDescriptor findService(String service, ConnectorServiceDescriptor csd) throws ConnectorFactoryException;
}
