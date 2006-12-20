/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.glue;

import org.mule.umo.lifecycle.InitialisationException;

import electric.glue.context.ServiceContext;
import electric.service.IService;

/**
 * <code>GlueServiceInitialisable</code> registers your service component to be
 * notified when it is being registered as a Glue soap service. Users can control how
 * the service behaves by manipulating the ServiceContext
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface GlueServiceInitialisable
{
    public void initialise(IService service, ServiceContext context) throws InitialisationException;
}
