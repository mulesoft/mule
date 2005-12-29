package org.mule.ide.core.jobs;

import java.util.Iterator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.Resource.Diagnostic;
import org.mule.ide.core.MuleCorePlugin;

/**
 * Background job for adding Eclipse markers based on the errors and warnings on an Ecore resource.
 */
public class UpdateMarkersForEcoreResourceJob extends Job {

	/** Eclipse resource to add markers to */
	private IResource eclipseResource;

	/** Ecore resource to load errors and warnings from */
	private Resource ecoreResource;

	public UpdateMarkersForEcoreResourceJob(IResource eclipseResource, Resource ecoreResource) {
		super("Updating markers for " + eclipseResource.getProjectRelativePath());
		this.eclipseResource = eclipseResource;
		this.ecoreResource = ecoreResource;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.internal.jobs.InternalJob#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IStatus run(IProgressMonitor monitor) {
		EList warnings = ecoreResource.getWarnings();
		EList errors = ecoreResource.getErrors();

		// Clear any existing markers.
		MuleCorePlugin.getDefault().clearMarkers(eclipseResource);

		// Add the warning markers.
		if (warnings.size() > 0) {
			Iterator it = warnings.iterator();
			while (it.hasNext()) {
				Diagnostic diag = (Diagnostic) it.next();
				Integer lineNumber = new Integer(diag.getLine());
				MuleCorePlugin.getDefault().createMarker(eclipseResource, IMarker.SEVERITY_WARNING,
						diag.getMessage(), lineNumber);
			}
		}

		// Add the error markers.
		if (errors.size() > 0) {
			Iterator it = errors.iterator();
			while (it.hasNext()) {
				Diagnostic diag = (Diagnostic) it.next();
				Integer lineNumber = new Integer(diag.getLine());
				MuleCorePlugin.getDefault().createMarker(eclipseResource, IMarker.SEVERITY_ERROR,
						diag.getMessage(), lineNumber);
			}
		}
		return Status.OK_STATUS;
	}
}