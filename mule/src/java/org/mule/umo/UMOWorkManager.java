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
package org.mule.umo;

import org.mule.umo.lifecycle.Startable;
import org.mule.umo.lifecycle.Lifecycle;

import javax.resource.spi.work.WorkManager;

/**
 * <code>UMOWorkManager</code> TODO
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public interface UMOWorkManager extends WorkManager, Lifecycle {
}
