/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.soap.glue;

import electric.glue.context.ServiceContext;
import electric.service.IService;
import org.mule.InitialisationException;

/**
 * <code>GlueServiceInitialisable</code> registers your service component to be
 * notified when it is being registered as a Glue soap service.
 * Users can control how the service behaves by manipulating the ServiceContext
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public interface GlueServiceInitialisable
{
    public void initialise(IService service, ServiceContext context) throws InitialisationException;
}
