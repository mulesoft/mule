/*
 * $Header$
 * $Revision$
 * $Date$
 */
package org.mule.ide.internal.core.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.mule.ide.core.model.IMuleConfigSet;
import org.mule.ide.core.model.IMuleConfiguration;
import org.mule.ide.core.model.IMuleModel;

/**
 * Default Mule model implementation.
 */
public class MuleModel implements IMuleModel {

	/** Map of Mule configurations hashed by unique id */
	private Map muleConfigurations = new HashMap();

	/** Map of Mule config sets hashed by unique id */
	private Map muleConfigSets = new HashMap();

	/** The project this model belongs to */
	private IProject project;

	/**
	 * Create a Mule IDE model for the given project.
	 * 
	 * @param project the project
	 */
	public MuleModel(IProject project) {
		this.project = project;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleModel#createNewMuleConfiguration(java.lang.String,
	 * java.lang.String)
	 */
	public IMuleConfiguration createNewMuleConfiguration(String description, String relativePath) {
		return new MuleConfiguration(this, getUniqueConfigurationId(), description, relativePath);
	}

	/**
	 * Find a unique id for a configuration within this model.
	 * 
	 * @return the unique id
	 */
	protected String getUniqueConfigurationId() {
		String base = "config";
		int index = 0;
		while (true) {
			String id = base + String.valueOf(index);
			if (getMuleConfiguration(id) == null) {
				return id;
			}
			index++;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleModel#createNewMuleConfigSet(java.lang.String)
	 */
	public IMuleConfigSet createNewMuleConfigSet(String description) {
		return new MuleConfigSet(this, getUniqueConfigSetId(), description);
	}

	/**
	 * Find a unique id for a config set within this model.
	 * 
	 * @return the unique id
	 */
	protected String getUniqueConfigSetId() {
		String base = "set";
		int index = 0;
		while (true) {
			String id = base + String.valueOf(index);
			if (getMuleConfigSet(id) == null) {
				return id;
			}
			index++;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleModel#getMuleConfigurations()
	 */
	public Collection getMuleConfigurations() {
		return muleConfigurations.values();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleModel#getMuleConfigurationForPath(org.eclipse.core.runtime.IPath)
	 */
	public IMuleConfiguration getMuleConfigurationForPath(IPath path) {
		Iterator it = getMuleConfigurations().iterator();
		while (it.hasNext()) {
			IMuleConfiguration config = (IMuleConfiguration) it.next();
			if (path.equals(config.getFilePath())) {
				return config;
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleModel#addMuleConfiguration(org.mule.ide.core.model.IMuleConfiguration)
	 */
	public void addMuleConfiguration(IMuleConfiguration muleConfiguration) {
		if (muleConfiguration == null) {
			throw new IllegalArgumentException("Attempted to add a null configuration.");
		}
		synchronized (this.muleConfigurations) {
			this.muleConfigurations.put(muleConfiguration.getId(), muleConfiguration);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleModel#getMuleConfiguration(java.lang.String)
	 */
	public IMuleConfiguration getMuleConfiguration(String id) {
		return (IMuleConfiguration) this.muleConfigurations.get(id);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleModel#removeMuleConfiguration(org.mule.ide.core.model.IMuleConfiguration)
	 */
	public IMuleConfiguration removeMuleConfiguration(String id) {
		if (id == null) {
			throw new IllegalArgumentException("Null id passed in removeMuleConfiguration.");
		}
		synchronized (this.muleConfigurations) {
			return (IMuleConfiguration) this.muleConfigurations.remove(id);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleModel#getMuleConfigSets()
	 */
	public Collection getMuleConfigSets() {
		return muleConfigSets.values();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleModel#addMuleConfigSet(org.mule.ide.core.model.IMuleConfigSet)
	 */
	public void addMuleConfigSet(IMuleConfigSet muleConfigSet) {
		if (muleConfigSet == null) {
			throw new IllegalArgumentException("Attempted to add a null config set.");
		}
		synchronized (this.muleConfigSets) {
			this.muleConfigSets.put(muleConfigSet.getId(), muleConfigSet);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleModel#getMuleConfigSet(java.lang.String)
	 */
	public IMuleConfigSet getMuleConfigSet(String id) {
		return (IMuleConfigSet) this.muleConfigSets.get(id);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleModel#removeMuleConfigSet(java.lang.String)
	 */
	public IMuleConfigSet removeMuleConfigSet(String id) {
		if (id == null) {
			throw new IllegalArgumentException("Null id passed in removeMuleConfigSet.");
		}
		synchronized (this.muleConfigurations) {
			return (IMuleConfigSet) this.muleConfigSets.remove(id);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleModel#getProject()
	 */
	public IProject getProject() {
		return this.project;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleModelElement#getMuleModel()
	 */
	public IMuleModel getMuleModel() {
		return this;
	}
}