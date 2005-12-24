package org.mule.ide.internal.core.classpath;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.mule.ide.core.MuleCorePlugin;

/**
 * Creates a classpath container for Mule libraries.
 */
public class MuleClasspathContainerInitializer extends ClasspathContainerInitializer {

	/**
	 * Indicates whether the given path represents the Mule classpath container.
	 * 
	 * @param path the container path
	 * @return true if it is the container
	 */
	protected boolean isMuleClasspathContainer(IPath path) {
		if ((path != null) && (path.segment(0).equals(MuleCorePlugin.ID_MULE_CLASSPATH_CONTAINER))) {
			return true;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.core.ClasspathContainerInitializer#initialize(org.eclipse.core.runtime.IPath,
	 * org.eclipse.jdt.core.IJavaProject)
	 */
	public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
		if (isMuleClasspathContainer(containerPath)) {
			MuleClasspathContainer container = new MuleClasspathContainer(project);
			JavaCore.setClasspathContainer(containerPath, new IJavaProject[] { project },
					new IClasspathContainer[] { container }, null);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.core.ClasspathContainerInitializer#canUpdateClasspathContainer(org.eclipse.core.runtime.IPath,
	 * org.eclipse.jdt.core.IJavaProject)
	 */
	public boolean canUpdateClasspathContainer(IPath containerPath, IJavaProject project) {
		return isMuleClasspathContainer(containerPath);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.core.ClasspathContainerInitializer#requestClasspathContainerUpdate(org.eclipse.core.runtime.IPath,
	 * org.eclipse.jdt.core.IJavaProject, org.eclipse.jdt.core.IClasspathContainer)
	 */
	public void requestClasspathContainerUpdate(IPath containerPath, IJavaProject project,
			IClasspathContainer containerSuggestion) throws CoreException {
		initialize(containerPath, project);
	}
}