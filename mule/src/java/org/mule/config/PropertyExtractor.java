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
package org.mule.config;

import org.mule.umo.UMOMessage;

/**
 * <code>PropertyExtractor</code> extracts a property from the message in a generic way.
 * i.e. composite properties can be pulled and aggregated depending on this strategy.  This
 * can be used to extract Correlation Ids, Message Ids etc.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public interface PropertyExtractor
{
    public Object getPropertry(String name, UMOMessage message);
}
