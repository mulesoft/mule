package org.mule.ide.internal.core.classpath;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.mule.ide.core.MuleCorePlugin;
import org.mule.ide.core.exception.MuleModelException;

/**
 * Classpath container that includes libraries required by Mule.
 */
public class MuleClasspathContainer implements IClasspathContainer {

	/** The description shown for the container */
	private static final String CONTAINER_DESC = "Mule Libraries";

	/** The path for the classpath container */
	private static final IPath PATH = new Path(MuleCorePlugin.ID_MULE_CLASSPATH_CONTAINER);

	/** The array of classpath entries */
	private IClasspathEntry[] entries;

	/**
	 * Create the classpath container given the Mule project.
	 * 
	 * @param project
	 */
	public MuleClasspathContainer(IJavaProject project) {
		reloadEntries(project);
	}

	/**
	 * Reload the entries for the given project.
	 * 
	 * @param project
	 */
	public void reloadEntries(IJavaProject javaProject) {
		try {
			if (MuleCorePlugin.getDefault().isExternalLibMuleClasspath()) {
				entries = MuleCorePlugin.getDefault().getExternalMuleLibraries(
						javaProject.getProject());
			} else {
				entries = MuleCorePlugin.getDefault().getMulePluginLibraries();
			}
		} catch (MuleModelException e) {
			entries = new IClasspathEntry[0];
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.core.IClasspathContainer#getClasspathEntries()
	 */
	public IClasspathEntry[] getClasspathEntries() {
		return entries;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.core.IClasspathContainer#getDescription()
	 */
	public String getDescription() {
		if (MuleCorePlugin.getDefault().isExternalLibMuleClasspath()) {
			return CONTAINER_DESC + " [External Classpath]";
		} else {
			return CONTAINER_DESC + " [Plugin Classpath]";
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.core.IClasspathContainer#getKind()
	 */
	public int getKind() {
		return IClasspathContainer.K_APPLICATION;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.core.IClasspathContainer#getPath()
	 */
	public IPath getPath() {
		return PATH;
	}
}