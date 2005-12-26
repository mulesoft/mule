/*
 * $Header$
 * $Revision$
 * $Date$
 */
package org.mule.ide.internal.core.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.mule.ide.core.model.IMuleConfigSet;
import org.mule.ide.core.model.IMuleConfiguration;
import org.mule.ide.core.model.IMuleModel;

/**
 * Default Mule config set implementation.
 */
public class MuleConfigSet extends MuleModelElement implements IMuleConfigSet {

	/** The parent model */
	private IMuleModel parent;

	/** Id */
	private String id;

	/** Description */
	private String description;

	/** Ordered list of configurations */
	private List muleConfigurations = new ArrayList();

	/**
	 * Create a config set.
	 * 
	 * @param parent the parent model
	 * @param id the unique id
	 * @param description the description
	 */
	public MuleConfigSet(IMuleModel parent, String id, String description) {
		this.parent = parent;
		this.id = id;
		this.description = description;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleConfigSet#getId()
	 */
	public String getId() {
		return this.id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleModelElement#getLabel()
	 */
	public String getLabel() {
		return getDescription();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleConfigSet#getDescription()
	 */
	public String getDescription() {
		return this.description;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleConfigSet#setDescription(java.lang.String)
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleConfigSet#getMuleConfigurations()
	 */
	public List getMuleConfigurations() {
		return this.muleConfigurations;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleConfigSet#getConfigFilePaths()
	 */
	public IPath[] getConfigFilePaths() {
		List result = new ArrayList();
		List configs = getMuleConfigurations();
		Iterator it = configs.iterator();
		while (it.hasNext()) {
			IMuleConfiguration config = (IMuleConfiguration) it.next();
			result.add(config.getFilePath());
		}
		return (IPath[]) result.toArray(new IPath[result.size()]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleConfigSet#isFirstConfiguration(org.mule.ide.core.model.IMuleConfiguration)
	 */
	public boolean isFirstConfiguration(IMuleConfiguration config) {
		if (config != null) {
			int numConfigs = getMuleConfigurations().size();
			if (numConfigs > 0) {
				IMuleConfiguration first = (IMuleConfiguration) getMuleConfigurations().get(0);
				if (config.getId().equals(first.getId())) {
					return true;
				}
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleConfigSet#isLastConfiguration(org.mule.ide.core.model.IMuleConfiguration)
	 */
	public boolean isLastConfiguration(IMuleConfiguration config) {
		if (config != null) {
			int numConfigs = getMuleConfigurations().size();
			if (numConfigs > 0) {
				IMuleConfiguration last = (IMuleConfiguration) getMuleConfigurations().get(
						numConfigs - 1);
				if (config.getId().equals(last.getId())) {
					return true;
				}
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleConfigSet#increasePriority(org.mule.ide.core.model.IMuleConfiguration)
	 */
	public void increasePriority(IMuleConfiguration config) {
		int location = getMuleConfigurations().indexOf(config);
		if (location > 0) {
			getMuleConfigurations().remove(location);
			getMuleConfigurations().add(location - 1, config);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleConfigSet#decreasePriority(org.mule.ide.core.model.IMuleConfiguration)
	 */
	public void decreasePriority(IMuleConfiguration config) {
		int location = getMuleConfigurations().indexOf(config);
		if ((location > -1) && (location < (getMuleConfigurations().size() - 1))) {
			getMuleConfigurations().remove(location);
			getMuleConfigurations().add(location + 1, config);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleConfigSet#addConfiguration(org.mule.ide.core.model.IMuleConfiguration)
	 */
	public void addConfiguration(IMuleConfiguration config) {
		if (!getMuleConfigurations().contains(config)) {
			getMuleConfigurations().add(config);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleConfigSet#removeConfiguration(org.mule.ide.core.model.IMuleConfiguration)
	 */
	public void removeConfiguration(IMuleConfiguration config) {
		int location = getMuleConfigurations().indexOf(config);
		if ((location > -1) && (location < getMuleConfigurations().size())) {
			getMuleConfigurations().remove(location);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleModelElement#getMuleModel()
	 */
	public IMuleModel getMuleModel() {
		return this.parent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleModelElement#refresh()
	 */
	public IStatus refresh() {
		return Status.OK_STATUS;
	}
}