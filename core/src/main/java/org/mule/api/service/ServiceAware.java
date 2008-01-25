/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.service;

import org.mule.api.config.ConfigurationException;

/**
 * <code>ServiceAware</code> is an injector interface that will supply a
 * Service to the object. This interface should be implemented by services
 * managed by Mule that want to receive their Service instance. The
 * Service will be set before any initialisation method is called. i.e. if the
 * service implements org.mule.api.lifecycle.Initialisable, the descriptor will be
 * set before initialise() method is called.
 * 
 * @see org.mule.api.lifecycle.Initialisable
 * @see Service
 */
public interface ServiceAware
{

    void setService(Service service) throws ConfigurationException;

}
