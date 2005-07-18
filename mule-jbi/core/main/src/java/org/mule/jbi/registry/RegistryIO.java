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
package org.mule.jbi.registry;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import org.mule.jbi.registry.impl.AssemblyImpl;
import org.mule.jbi.registry.impl.BindingImpl;
import org.mule.jbi.registry.impl.EngineImpl;
import org.mule.jbi.registry.impl.LibraryImpl;
import org.mule.jbi.registry.impl.RegistryImpl;
import org.mule.jbi.registry.impl.UnitImpl;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public class RegistryIO {

	public static void save(Registry registry) throws IOException {
		synchronized (registry) {
			try {
				Writer w = new FileWriter(registry.getStore());
				getXStream().toXML(registry, w);
				w.close();
			} catch (Exception e) {
				if (e instanceof IOException) {
					throw (IOException) e;
				}
				throw (IOException) new IOException("Could not save registry").initCause(e);
			}
		}
	}
	

	public static Registry load(File store) throws IOException {
		Reader r = new FileReader(store);
		RegistryImpl reg = (RegistryImpl) getXStream().fromXML(r);
		reg.initialize();
		reg.setStore(store);
		r.close();
		return reg;
	}
	
	public static Registry create(File store) throws IOException {
		RegistryImpl reg = new RegistryImpl();
		reg.initialize();
		reg.setStore(store);
		save(reg);
		return reg;
	}
	
	private static XStream getXStream() {
		if (xstream == null) {
			xstream = new XStream(new StaxDriver());
			xstream.alias("registry", RegistryImpl.class);
			xstream.alias("engine", EngineImpl.class);
			xstream.alias("binding", BindingImpl.class);
			xstream.alias("library", LibraryImpl.class);
			xstream.alias("assembly", AssemblyImpl.class);
			xstream.alias("unit", UnitImpl.class);
		}
		return xstream;
	}
	
	private static transient XStream xstream;
}
