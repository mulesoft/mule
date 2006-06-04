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
*
*/
package org.mule.test.integration.config;

import org.mule.components.simple.EchoComponent;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.jndi.SimpleContext;
import org.mule.management.agents.Log4jAgent;
import org.mule.providers.vm.VMConnector;
import org.mule.transformers.xml.XmlToObject;
import org.mule.util.ObjectFactory;

/**
 * A dummy property factory for creating a Jndi context
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class DummyInitialContextFactory implements ObjectFactory {
    public Object create() throws Exception {
        SimpleContext c = new SimpleContext();
        c.bind("vmConnector", new VMConnector());
        c.bind("endpointRef", "vm://my.object");
        c.bind("Log4JAgent", new Log4jAgent());
        c.bind("XmlToObject", new XmlToObject());
        MuleDescriptor d = new MuleDescriptor("EchoUMO");
        d.setImplementation("echoBean");
        c.bind("EchoUMO", d);
        c.bind("echoBean", new EchoComponent());
        return c;
    }
}
