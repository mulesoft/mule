/*
 * $Id: MuleEventListener.java 2179 2006-06-04 22:51:52Z holger $
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.extras.spring.events;

import org.springframework.context.ApplicationListener;

/**
 * <code>MuleEventListener</code> is a interface that identifies an object as
 * wanting to receive Mule Events
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision: 2179 $
 */

public interface MuleEventListener extends ApplicationListener
{
    // just a marker
}
