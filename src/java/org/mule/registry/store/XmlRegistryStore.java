/*
 * Copyright 2005 SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 * 
 * ------------------------------------------------------------------------------------------------------
 * $Header$
 * $Revision$
 * $Date$
 */
package org.mule.registry.store;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import org.mule.registry.Registry;
import org.mule.registry.RegistryException;
import org.mule.registry.RegistryFactory;
import org.mule.registry.RegistryStore;
import org.mule.registry.impl.AbstractRegistry;
import org.mule.ManagementContext;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class XmlRegistryStore implements RegistryStore {

    protected ManagementContext context;

    public XmlRegistryStore(ManagementContext context) {
        this.context = context;
    }

	public void save(Registry registry) throws RegistryException {
		synchronized (registry) {
			try {
				Writer w = new FileWriter(new File(registry.getStoreLocation()));
				getXStream().toXML(registry, w);
				w.close();
			} catch (Exception e) {
				throw new RegistryException("Could not save registry", e);
			}
		}
	}
	

	public Registry load(String storeLocation) throws RegistryException {
        try {
            Reader r = new FileReader(storeLocation);
            AbstractRegistry reg = (AbstractRegistry) getXStream().fromXML(r);
            reg.initialize();
            reg.setStoreLocation(storeLocation);
            r.close();
            return reg;
        } catch (IOException e) {
            throw new RegistryException("Could not load registry", e);
        }
    }
	
	public Registry create(String store, RegistryFactory factory) throws RegistryException {
		Registry reg = factory.create(this, context);
        if(reg instanceof AbstractRegistry) {
            ((AbstractRegistry)reg).initialize();
            ((AbstractRegistry)reg).setStoreLocation(store);
        }
		save(reg);
		return reg;
	}
	
	private static XStream getXStream() {
		if (xstream == null) {
			xstream = new XStream(new StaxDriver());
//			xstream.alias("registry", BaseRegistry.class);
//			xstream.alias("engine", EngineImpl.class);
//			xstream.alias("binding", BindingImpl.class);
//			xstream.alias("library", LibraryImpl.class);
//			xstream.alias("assembly", AssemblyImpl.class);
//			xstream.alias("unit", UnitImpl.class);
		}
		return xstream;
	}
	
	private static transient XStream xstream;
}
