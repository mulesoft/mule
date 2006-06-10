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
package org.mule.umo.lifecycle;

/**
 * <code>UMOEventListener</code> is a marker interface that is implemented by
 * ojects wishing to receive UMO events in managed environments, succh as an ejb
 * container. There are not methods on this interface a typically Mule will work
 * out the method to invoke on the listener. Developers can implement
 * <code>org.mule.umo.lifecycle.Callable</code> to implement a spcific Mule
 * listener interface.
 * 
 * @see Callable
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface UMOEventListener
{
    // no methods
}
