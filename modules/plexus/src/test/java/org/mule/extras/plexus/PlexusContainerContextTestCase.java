/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extras.plexus;

import org.mule.config.ConfigurationException;
import org.mule.tck.model.AbstractContainerContextTestCase;
import org.mule.umo.manager.UMOContainerContext;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class PlexusContainerContextTestCase extends AbstractContainerContextTestCase
{
    public UMOContainerContext getContainerContext() throws ConfigurationException
    {
        PlexusContainerContext context = new PlexusContainerContext();
        context.setConfigFile("test-plexus-config.xml");
        return context;
    }
}
