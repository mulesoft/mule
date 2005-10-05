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
package org.mule.jbi.registry;

import org.mule.registry.Registry;
import org.mule.registry.RegistryComponent;
import org.mule.registry.impl.AbstractUnit;

import javax.jbi.component.Component;
import javax.jbi.component.ServiceUnitManager;

/**
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class JbiUnit extends AbstractUnit {

    public JbiUnit(Registry registry) {
        super(registry);
    }

    protected void doInit() throws Exception {
        getServiceUnitManager().init(getName(), getInstallRoot());
    }

    protected void doStart() throws Exception {
        getServiceUnitManager().start(getName());
    }

    protected void doStop() throws Exception {
        getServiceUnitManager().stop(getName());
    }

    protected void doShutDown() throws Exception {
        getServiceUnitManager().shutDown(getName());
    }

    public String doDeploy() throws Exception {
        String result = getServiceUnitManager().deploy(getName(), getInstallRoot());
		getServiceUnitManager().init(getName(), getInstallRoot());
        return result;
    }

    protected String doUndeploy() throws Exception {
        String result = getServiceUnitManager().undeploy(getName(), getInstallRoot());
		getServiceUnitManager().init(getName(), getInstallRoot());
        return result;
    }

    protected ServiceUnitManager getServiceUnitManager() {
		return ((Component)getRegistryComponent().getComponent()).getServiceUnitManager();
	}

    public RegistryComponent getRegistryComponent() {
        return registry.getComponent(getName());
    }
}
