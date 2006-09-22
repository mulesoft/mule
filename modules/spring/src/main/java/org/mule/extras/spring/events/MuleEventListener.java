/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extras.spring.events;

import org.springframework.context.ApplicationListener;

/**
 * <code>MuleEventListener</code> is a interface that identifies an object as
 * wanting to receive Mule Events
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public interface MuleEventListener extends ApplicationListener
{
    // just a marker
}
