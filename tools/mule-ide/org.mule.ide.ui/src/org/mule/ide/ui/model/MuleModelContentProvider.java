/*
 * $Header$
 * $Revision$
 * $Date$
 */
package org.mule.ide.ui.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.mule.ide.core.model.IMuleConfigSet;
import org.mule.ide.core.model.IMuleModel;

/**
 * Provides content hierarchy for the Mule model.
 */
public class MuleModelContentProvider implements IStructuredContentProvider {

	/** Constant for no children */
	private static final Object[] NO_CHILDREN = new Object[0];

	/** Indicates whether configuration files appear in the tree */
	private boolean showingConfigurations;

	/** Indicates whether config sets appear in the tree */
	private boolean showingConfigSets;

	public MuleModelContentProvider(boolean showConfigurations, boolean showConfigSets) {
		this.setShowingConfigurations(showConfigurations);
		this.setShowingConfigSets(showConfigSets);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof IMuleModel) {
			IMuleModel model = (IMuleModel) inputElement;
			List children = new ArrayList();
			if (isShowingConfigurations()) {
				children.addAll(model.getMuleConfigurations());
			}
			if (isShowingConfigSets()) {
				children.addAll(model.getMuleConfigSets());
			}
			return children.toArray();
		} else if (inputElement instanceof IMuleConfigSet) {
			return ((IMuleConfigSet) inputElement).getMuleConfigurations().toArray();
		}
		return NO_CHILDREN;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	public void dispose() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer,
	 * java.lang.Object, java.lang.Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	/**
	 * Sets the 'showingConfigurations' field.
	 * 
	 * @param showingConfigurations The 'showingConfigurations' value.
	 */
	protected void setShowingConfigurations(boolean showingConfigurations) {
		this.showingConfigurations = showingConfigurations;
	}

	/**
	 * Returns the 'showingConfigurations' field.
	 * 
	 * @return the 'showingConfigurations' field value.
	 */
	public boolean isShowingConfigurations() {
		return showingConfigurations;
	}

	/**
	 * Sets the 'showingConfigSets' field.
	 * 
	 * @param showingConfigSets The 'showingConfigSets' value.
	 */
	protected void setShowingConfigSets(boolean showingConfigSets) {
		this.showingConfigSets = showingConfigSets;
	}

	/**
	 * Returns the 'showingConfigSets' field.
	 * 
	 * @return the 'showingConfigSets' field value.
	 */
	public boolean isShowingConfigSets() {
		return showingConfigSets;
	}
}