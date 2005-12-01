/*
 * $Header$
 * $Revision$
 * $Date$
 */
package org.mule.ide.internal.core.model;

import java.util.ArrayList;
import java.util.List;

import org.mule.ide.core.model.IMuleConfigSet;

/**
 * Default Mule config set implementation.
 */
public class MuleConfigSet implements IMuleConfigSet {

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
}