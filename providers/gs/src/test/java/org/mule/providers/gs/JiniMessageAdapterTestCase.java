/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.gs;

import org.mule.tck.providers.AbstractMessageAdapterTestCase;
import org.mule.umo.provider.UMOMessageAdapter;

/**
 * @version $Revision$
 */
public class JiniMessageAdapterTestCase extends AbstractMessageAdapterTestCase
{
    public Object getValidMessage() throws Exception
    {
        return new JiniMessage(null, "hello");
    }

    public Object getInvalidMessage() {
        return null;
    }

    public UMOMessageAdapter createAdapter(Object payload) throws Exception
    {
        return new JiniMessageAdapter(payload);
    }
}
