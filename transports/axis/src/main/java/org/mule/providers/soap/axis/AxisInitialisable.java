/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.axis;

import org.apache.axis.handlers.soap.SOAPService;
import org.mule.umo.lifecycle.InitialisationException;

/**
 * <code>AxisInitialisable</code> can be implemented by a Mule component that will
 * be used as an Axis service to customise the Axis Service object
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface AxisInitialisable
{
    public void initialise(SOAPService service) throws InitialisationException;
}
