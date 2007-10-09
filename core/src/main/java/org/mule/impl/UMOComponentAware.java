/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl;

import org.mule.config.ConfigurationException;
import org.mule.umo.UMOComponent;

/**
 * <code>UMOComponentAware</code> is an injector interface that will supply a
 * UMOComponent to the object. This interface should be implemented by services
 * managed by Mule that want to receive their UMOComponent instance. The
 * UMOComponent will be set before any initialisation method is called. i.e. if the
 * component implements org.mule.umo.lifecycle.Initialisable, the descriptor will be
 * set before initialise() method is called.
 * 
 * @see org.mule.umo.lifecycle.Initialisable
 * @see UMOComponent
 */
public interface UMOComponentAware
{

    void setComponent(UMOComponent component) throws ConfigurationException;

}
