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
*
*/
package org.mule.jbi.components.mule;

import org.mule.impl.MuleDescriptor;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.RecoverableException;

/**
 * A null component is used when interfacing with JBI components, the Null
 * component is a placeholder of the JBI component that isn't managed
 * by mule
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class NullUMOComponent implements UMOComponent
{
    private UMODescriptor descriptor;

    public NullUMOComponent(String name) {
        this.descriptor = new MuleDescriptor(name);
    }

    public UMODescriptor getDescriptor() {
        return descriptor;
    }

    public void dispatchEvent(UMOEvent event) throws UMOException {
        throw new UnsupportedOperationException("NullComponent:dispatchEvent");
    }

    public UMOMessage sendEvent(UMOEvent event) throws UMOException {
        throw new UnsupportedOperationException("NullComponent:sendEvent");
    }

    public void pause() throws UMOException {

    }

    public void resume() throws UMOException {

    }

    public void start() throws UMOException {

    }

    public void stop() throws UMOException {

    }

    public void dispose() {

    }

    public void initialise() throws InitialisationException, RecoverableException {

    }
}
