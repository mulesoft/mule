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
package org.mule.umo.space;

import java.util.EventObject;

/**
 * A space event occurs when an item is added to the sapce and there is a one or more listeners waiting for it.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class UMOSpaceEvent extends EventObject {

    private UMOSpace space;

    public UMOSpaceEvent(Object item, UMOSpace space) {
        super(item);
        this.space = space;
    }

    public UMOSpace getSpace() {
        return space;
    }
}
