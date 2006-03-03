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
package org.mule.registry.impl;

import org.mule.ManagementContext;
import org.mule.MuleManager;
import org.mule.registry.Assembly;
import org.mule.registry.ComponentType;
import org.mule.registry.Library;
import org.mule.registry.Registry;
import org.mule.registry.RegistryComponent;
import org.mule.registry.RegistryException;
import org.mule.registry.RegistryStore;
import org.mule.registry.Unit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public abstract class AbstractRegistry implements Registry {

    public static final String REGISTRY_DIRECTORY = "registry";

	private List libraries;
	private List assemblies;
	private List components;
	private transient Map registry;
	private transient Map librariesMap;
	private transient Map assembliesMap;
	private transient Map componentsMap;
	private transient String storeLocation;
    private File workingDirectory;
    protected transient RegistryStore store;
    protected transient ManagementContext context;

    private transient boolean started = false;

	public AbstractRegistry(RegistryStore store, ManagementContext context) {
        this.store = store;
        this.context = context;
        registry = new HashMap();
        for (int i = 0; i < ComponentType.COMPONENT_TYPES.length; i++) {
            ComponentType componentType = ComponentType.COMPONENT_TYPES[i];
            registry.put(componentType.getName() + ".list", new ArrayList());
            registry.put(componentType.getName() + ".map", new HashMap());
        }

		this.components = new ArrayList();
		this.libraries = new ArrayList();
		this.assemblies = new ArrayList();
		this.librariesMap = new HashMap();
		this.assembliesMap = new HashMap();
		this.componentsMap = new HashMap();

        workingDirectory = new File(MuleManager.getConfiguration().getWorkingDirectory(), REGISTRY_DIRECTORY);
	}

    public File getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

	public String getStoreLocation() {
		return this.storeLocation;
	}


	public void setStoreLocation(String storeLocation) {
		this.storeLocation = storeLocation;
	}
	

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Registry#getComponents()
	 */
	public synchronized RegistryComponent[] getComponents() {
		return (RegistryComponent[]) components.toArray(new RegistryComponent[components.size()]);
	}

    public synchronized RegistryComponent[] getComponents(ComponentType type) {
        RegistryComponent[] components = new RegistryComponent[]{};
        List list = (List)registry.get(type + ".list");
        if(list!=null) {
		    components = (RegistryComponent[]) list.toArray(new RegistryComponent[list.size()]);
        }
        return components;
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Registry#getRegistryComponent(java.lang.String)
	 */
	public synchronized RegistryComponent getComponent(String name, ComponentType type) {
		return (RegistryComponent) this.componentsMap.get(name);
	}


	public synchronized void removeComponent(RegistryComponent component) {
		this.componentsMap.remove(component.getName());
        List list = (List)registry.get(component.getType() + ".list");
        if(list!=null) {
            list.remove(component);
        }
        Map map = (Map)registry.get(component.getType() + ".map");
        if(map!=null) {
            map.remove(component);
        }
	}
	
	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Registry#getRegistryComponent(java.lang.String)
	 */
	public synchronized RegistryComponent getComponent(String name) {
		return (RegistryComponent) this.componentsMap.get(name);
	}


	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Registry#addEngine(java.lang.String)
	 */
	public synchronized RegistryComponent addComponent(String name, ComponentType type) throws RegistryException {
		if (getComponent(name) != null) {
			throw new RegistryException("Component already registered: " + name);
		}
		RegistryComponent rc = createComponent(name, type);
		this.componentsMap.put(name, rc);
		return rc;
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Registry#addTransientEngine(java.lang.String, javax.jbi.component.Component)
	 */
	public synchronized RegistryComponent addTransientComponent(String name, ComponentType type, Object component, Object bootstrap) throws RegistryException {

        RegistryComponent rc = getComponent(name);
		if (rc == null) {
		    rc = createComponent(name, type);
			rc.setTransient(true);
			rc.setComponent(component);
			rc.setStateAtShutdown(RegistryComponent.RUNNING);
            try {
                rc.setWorkspaceRoot(context.getComponentWorkspaceDir(getWorkingDirectory(), name).getAbsoluteFile().getCanonicalPath());
            } catch (IOException e) {
                throw new RegistryException(e);
            }
            this.componentsMap.put(name, rc);
            components.add(rc);
			if (bootstrap != null) {
                try {
                    bootstrapComponent(rc, bootstrap);
                } catch (Exception e) {
                    throw new RegistryException(e);
                }
            }
		} else {
			if ( !rc.isTransient()) {
				throw new RegistryException("A non-transient component is already registered: " + name);
			}
			rc.setComponent(component);
		}
        try {
            rc.initComponent();
        } catch (Exception e) {
            throw new RegistryException(e);
        }
        return rc;
	}

    protected abstract void bootstrapComponent(RegistryComponent component, Object bootstrap) throws Exception;


	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Registry#getLibraries()
	 */
	public synchronized Library[] getLibraries() {
		Collection c = this.libraries;
		return (Library[]) c.toArray(new Library[c.size()]);
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Registry#getLibrary(java.lang.String)
	 */
	public synchronized Library getLibrary(String name) {
		return (Library) this.librariesMap.get(name);
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Registry#addLibrary(java.lang.String)
	 */
	public synchronized Library addLibrary(String name) throws RegistryException {
		if (getLibrary(name) != null) {
			throw new RegistryException("Library already registered: " + name);
		}
		Library l = createLibrary(name);
		this.libraries.add(l);
		this.librariesMap.put(name, l);
		return l;
	}
	
	public synchronized void removeLibrary(Library library) {
		this.librariesMap.remove(library.getName());
		this.libraries.remove(library);
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Registry#getAssemblies()
	 */
	public synchronized Assembly[] getAssemblies() {
		Collection c = this.assemblies;
		return (Assembly[]) c.toArray(new Assembly[c.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Registry#getAssembly(java.lang.String)
	 */
	public synchronized Assembly getAssembly(String name) {
		return (Assembly) this.assembliesMap.get(name);
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Registry#addAssembly(java.lang.String)
	 */
	public synchronized Assembly addAssembly(String name) {
		if (getAssembly(name) != null) {
			return null;
		}
		Assembly a = createAssembly(name);
		this.assemblies.add(a);
		this.assembliesMap.put(name, a);
		return a;
	}

	public synchronized void removeAssembly(Assembly assembly) {
		this.assembliesMap.remove(assembly.getName());
		this.assemblies.remove(assembly);
	}

	public void initialize() {
		this.componentsMap = new HashMap();
		for (Iterator it = this.components.iterator(); it.hasNext();) {
			RegistryComponent e = (RegistryComponent) it.next();
			this.componentsMap.put(e.getName(), e);
		}
		this.librariesMap = new HashMap();
		for (Iterator it = this.libraries.iterator(); it.hasNext();) {
			AbstractLibrary l = (AbstractLibrary) it.next();
			this.librariesMap.put(l.getName(), l);
		}
		this.assembliesMap = new HashMap();
		for (Iterator it = this.assemblies.iterator(); it.hasNext();) {
			AbstractAssembly a = (AbstractAssembly) it.next();
			this.assembliesMap.put(a.getName(), a);
		}
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Registry#start()
	 */
	public synchronized void start() throws RegistryException {
		started = true;
        try {
			RegistryComponent[] components = getComponents();
			for (int i = 0; i < components.length; i++) {
				if (components[i].isTransient() && components[i].getComponent() == null) {
					// a transient component was removed from config, so remove it from registry
					removeComponent(components[i]);
				} else {
					components[i].restoreState();
				}
			}
			Assembly[] assemblies = getAssemblies();
			for (int i = 0; i < assemblies.length; i++) {
				assemblies[i].restoreState();
			}
		} catch (Exception e) {
			throw new RegistryException(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Registry#shutDown()
	 */
	public synchronized void shutDown() throws RegistryException {
			RegistryComponent[] components = getComponents();
			for (int i = 0; i < components.length; i++) {
				components[i].saveAndShutdown();
			}
			store.save(this);
	}
	
	/* (non-Javadoc)
	 * @see org.mule.jbi.registry.Registry#addTransientUnit(java.lang.String, java.lang.String, java.lang.String)
	 */
	public synchronized void addTransientUnit(String suName, RegistryComponent component, String installDir) throws RegistryException {
		Assembly a = getAssembly(suName);
		if (a == null) {
			Assembly assembly = createAssembly(suName);
			assembly.setTransient(true);
			assembly.setStateAtShutdown(Assembly.RUNNING);
			Unit unit = createUnit(suName);
			unit.setName(suName);
			unit.setAssembly(assembly);
			unit.setRegistryComponent(component);
            try {
                unit.setInstallRoot(new File(installDir).getAbsoluteFile().getCanonicalPath());
            } catch (IOException e) {
                throw new RegistryException(e);
            }
            this.assemblies.add(assembly);
			this.assembliesMap.put(suName, assembly);
			unit.deploy();
            unit.start();
            assembly.setCurrentState(Assembly.RUNNING);
		} else {
			if (!a.isTransient()) {
				throw new RegistryException("A non-transient or assembly is already deployed: " + suName);
			}
		}
	}

    public void save() throws RegistryException {
        store.save(this);
    }

    public boolean isStarted() {
        return started;
    }

    public ManagementContext getManagementContext() {
        return context;
    }
}
