/*
 * $Header$
 * $Revision$
 * $Date$
 */
package org.mule.ide.ui.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.mule.ide.core.model.IMuleModel;
import org.mule.ide.core.model.IMuleModelElement;

/**
 * Provides content hierarchy for the Mule model.
 */
public class MuleModelContentProvider implements ITreeContentProvider {

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
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IMuleModel) {
			IMuleModel model = (IMuleModel) parentElement;
			List children = new ArrayList();
			if (isShowingConfigurations()) {
				children.addAll(model.getMuleConfigurations());
			}
			if (isShowingConfigSets()) {
				children.addAll(model.getMuleConfigSets());
			}
			return children.toArray();
		}
		return NO_CHILDREN;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	public Object getParent(Object element) {
		if ((element instanceof IMuleModelElement) && (!(element instanceof IMuleModel))) {
			return ((IMuleModelElement) element).getMuleModel();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
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