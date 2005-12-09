/**
 * Copyright (C) 2003-2005, Cox Communications, Inc.
 */
package org.mule.ide.core.jobs;

import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.mule.ide.core.MuleCorePlugin;
import org.mule.ide.core.exception.MuleModelException;
import org.mule.ide.core.model.IMuleConfiguration;
import org.mule.ide.core.model.IMuleModel;

/**
 * Background job for refreshing the content model from the Mule configuration files.
 */
public class RefreshMuleConfigurationsJob extends Job {

	/** The model to refresh */
	private IMuleModel model;

	public RefreshMuleConfigurationsJob(IMuleModel model) {
		super("Refreshing Mule configuration files");
		this.model = model;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.internal.jobs.InternalJob#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IStatus run(IProgressMonitor monitor) {
		MultiStatus status = MuleCorePlugin.getDefault().createMultiStatus(
				"Errors loading Mule configuration files.");
		int numConfigs = model.getMuleConfigurations().size();
		Iterator it = model.getMuleConfigurations().iterator();
		monitor.beginTask("Refreshing Mule configuration files...", numConfigs);
		while (it.hasNext()) {
			// Getting the config forces a refresh if not initialized.
			IMuleConfiguration config = (IMuleConfiguration) it.next();
			try {
				config.getConfigDocument();
			} catch (MuleModelException e) {
				status.add(e.getStatus());
			}
			monitor.worked(1);
		}
		return status;
	}
}