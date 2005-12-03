/*
 * $Header$
 * $Revision$
 * $Date$
 */
package org.mule.ide.internal.core.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.mule.ide.core.model.IMuleConfigSet;
import org.mule.ide.core.model.IMuleModel;

/**
 * Default Mule config set implementation.
 */
public class MuleConfigSet extends MuleModelElement implements IMuleConfigSet {

	/** The parent model */
	private MuleModel parent;

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
	public MuleConfigSet(MuleModel parent, String id, String description) {
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
	 * @see org.mule.ide.core.model.IMuleConfigSet#getDescription()
	 */
	public String getDescription() {
		return this.description;
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