package org.mule.ide.internal.core.classpath;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.mule.ide.core.MuleCorePlugin;
import org.mule.ide.core.exception.MuleModelException;
import org.mule.ide.core.nature.MuleNature;

/**
 * Utility functions for establishing the Mule classpath
 * 
 * @author Derek Adams
 */
public class MuleClasspathUtils {

	/**
	 * Create a container entry for the Mule classpath manager.
	 * 
	 * @return the classpath entry
	 */
	public static IClasspathEntry createMuleClasspathContainerEntry() {
		return JavaCore.newContainerEntry(new Path(MuleCorePlugin.ID_MULE_CLASSPATH_CONTAINER));
	}

	/**
	 * Create classpath entries for all jars in the "lib" folder of the plugin.
	 * 
	 * @return the array of entries
	 */
	public static IClasspathEntry[] getMulePluginLibraryEntries() {
		Enumeration urls = MuleCorePlugin.getDefault().getBundle().findEntries("/", "*.jar", false);
		return convertUrlsToClasspathEntries(urls);
	}

	/**
	 * Create a classpath entry for the mule core jar.
	 * 
	 * @return the entry, or null if not found
	 */
	public static IClasspathEntry getMuleCoreLibraryEntry() {
		Enumeration urls = MuleCorePlugin.getDefault().getBundle().findEntries("/",
				"mule-core.jar", false);
		IClasspathEntry[] entries = convertUrlsToClasspathEntries(urls);
		if (entries.length > 0) {
			return entries[0];
		} else {
			return null;
		}
	}

	/**
	 * Convert plugin URLs to classpath entries.
	 * 
	 * @param urls the URLs
	 * @return the classpath entries
	 */
	protected static IClasspathEntry[] convertUrlsToClasspathEntries(Enumeration urls) {
		if (urls == null) {
			return new IClasspathEntry[0];
		}
		List classpath = new ArrayList();
		while (urls.hasMoreElements()) {
			URL url = (URL) urls.nextElement();
			try {
				// url = Platform.resolve(url);
				url = Platform.asLocalURL(url);
				// IPath filePath = new Path(new File(url.getFile()).getAbsolutePath());
				IPath filePath = new Path(url.getFile());
				IClasspathEntry entry = JavaCore.newLibraryEntry(filePath, null, null);
				classpath.add(entry);
			} catch (IOException e) {
				MuleCorePlugin.getDefault().logException(
						"Unable to access library: " + url.toString(), e);
			}
		}
		return (IClasspathEntry[]) classpath.toArray(new IClasspathEntry[classpath.size()]);
	}

	/**
	 * Get the libraries located in an external Mule installation folder.
	 * 
	 * @param project the project that will contain the classpath entries
	 * @return the classpath entries for libraries in the install folder
	 * @throws MuleModelException if there is a problem accessing the external location
	 */
	public static IClasspathEntry[] getExternalMuleLibraries(IProject project)
			throws MuleModelException {
		// Make sure the external root folder is specified in preferences.
		if (!MuleCorePlugin.getDefault().hasExternalMuleRootVariable()) {
			MuleCorePlugin.getDefault().updateExternalMuleRootVariable();
		}

		// Load the Mule nature for the project.
		MuleNature nature = MuleCorePlugin.getDefault().getMuleNature(project);
		if (nature == null) {
			throw new MuleModelException(MuleCorePlugin.getDefault().createErrorStatus(
					"Project does not have a Mule nature.", null));
		}

		// Get the linked folder and create the classpath from its jars.
		IFolder folder = nature.getExternalLibFolder();
		if (!folder.exists()) {
			throw new MuleModelException(MuleCorePlugin.getDefault().createErrorStatus(
					"External Mule lib folder not found", null));
		}
		try {
			List entries = new ArrayList();
			IResource[] members = folder.members();
			for (int i = 0; i < members.length; i++) {
				if ((members[i].getType() == IResource.FILE)
						&& ("jar".equals(members[i].getFileExtension()))) {
					IClasspathEntry entry = JavaCore.newLibraryEntry(members[i].getRawLocation(),
							null, null);
					entries.add(entry);
				}
			}

			// Add the core library to the classpath.
			IClasspathEntry entry = getMuleCoreLibraryEntry();
			if (entry != null) {
				entries.add(entry);
			}
			return (IClasspathEntry[]) entries.toArray(new IClasspathEntry[entries.size()]);
		} catch (CoreException e) {
			throw new MuleModelException(e.getStatus());
		}
	}
}