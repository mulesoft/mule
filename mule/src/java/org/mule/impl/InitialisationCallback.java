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
package org.mule.impl;

import org.mule.InitialisationException;

/**
 * <code>InitialisationCallback</code> is used to provide customised initialiation
 * for more complex components. For example, soap services have a custom initialisation
 * that passes the service object to the mule component.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public interface InitialisationCallback
{
    public void initialise(Object component) throws InitialisationException;
}
