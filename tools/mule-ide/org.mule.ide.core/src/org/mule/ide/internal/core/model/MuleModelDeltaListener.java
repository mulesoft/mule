/**
 * Copyright (C) 2003-2005, Cox Communications, Inc.
 */
package org.mule.ide.internal.core.model;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.mule.ide.core.IMuleDefaults;
import org.mule.ide.core.MuleCorePlugin;
import org.mule.ide.core.model.IMuleModel;

/**
 * Looks for resource deltas that Mule IDE is interested in.
 */
public class MuleModelDeltaListener implements IResourceChangeListener, IResourceDeltaVisitor {

	/** The Mule model affected */
	private IMuleModel model;

	/** Path to Mule IDE config file */
	private IPath configFilePath;

	public MuleModelDeltaListener(IMuleModel model) {
		this.setModel(model);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		try {
			event.getDelta().accept(this);
		} catch (CoreException e) {
			MuleCorePlugin.getDefault().getLog().log(e.getStatus());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.core.resources.IResourceDelta)
	 */
	public boolean visit(IResourceDelta delta) throws CoreException {
		IResource resource = delta.getResource();
		switch (resource.getType()) {
		case IResource.FILE: {
			switch (delta.getKind()) {
			case IResourceDelta.CHANGED:
				if (resource.getFullPath().equals(getConfigFilePath())) {
					// Ignore marker changes.
					if ((delta.getFlags() & IResourceDelta.MARKERS) == 0) {
						muleIdeConfigFileChanged();
					}
				}
			}
		}
		}
		return true;
	}

	/**
	 * Called when the contents of the Mule IDE config file changes (the .muleide file).
	 */
	protected void muleIdeConfigFileChanged() {
		getModel().refresh();
	}

	/**
	 * Sets the 'configFilePath' field.
	 * 
	 * @param configFilePath The 'configFilePath' value.
	 */
	protected void setConfigFilePath(IPath configFilePath) {
		this.configFilePath = configFilePath;
	}

	/**
	 * Returns the 'configFilePath' field.
	 * 
	 * @return the 'configFilePath' field value.
	 */
	protected IPath getConfigFilePath() {
		if (configFilePath == null) {
			IResource resource = getModel().getProject().findMember(
					IMuleDefaults.MULE_IDE_CONFIG_FILENAME);
			if (resource != null) {
				configFilePath = resource.getFullPath();
			}
		}
		return configFilePath;
	}

	/**
	 * Sets the 'model' field.
	 * 
	 * @param model The 'model' value.
	 */
	protected void setModel(IMuleModel model) {
		this.model = model;
	}

	/**
	 * Returns the 'model' field.
	 * 
	 * @return the 'model' field value.
	 */
	protected IMuleModel getModel() {
		return model;
	}
}