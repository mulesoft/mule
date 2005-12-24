package org.mule.ide.internal.core.classpath;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.mule.ide.core.MuleCorePlugin;

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
	public void reloadEntries(IJavaProject project) {
		entries = MuleCorePlugin.getDefault().getMuleLibraries();
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
		return CONTAINER_DESC;
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