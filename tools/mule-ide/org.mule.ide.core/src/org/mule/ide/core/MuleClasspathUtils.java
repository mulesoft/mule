package org.mule.ide.core;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.mule.ide.core.exception.MuleModelException;

/**
 * Utility functions for establishing the Mule classpath
 * 
 * @author Derek Adams
 */
public class MuleClasspathUtils {

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
	 * @param location the path to the external Mule folder
	 * @return the classpath entries for libraries in the install folder
	 * @throws MuleModelException if there is a problem accessing the external location
	 */
	public static IClasspathEntry[] getExternalMuleLibraries(String location)
			throws MuleModelException {
		IPath basePath = new Path(location);
		IPath libPath = basePath.append("lib");
		IPath sourcePath = basePath.append("src").append("java");
		IPath javadocPath = basePath.append("docs").append("apidocs");
		String javadocPathURI = javadocPath.toFile().toURI().toString();
		IClasspathAttribute jdocAttr = JavaCore.newClasspathAttribute(
				IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, javadocPathURI);
		IClasspathAttribute[] muleAttrs = new IClasspathAttribute[] { jdocAttr };
		File externalRoot = libPath.toFile();
		if (!externalRoot.exists()) {
			throw new MuleModelException(MuleCorePlugin.getDefault().createErrorStatus(
					"External Mule root is invalid", null));
		}
		List entries = new ArrayList();
		File[] members = externalRoot.listFiles();
		for (int i = 0; i < members.length; i++) {
			File file = members[i];
			if (file.getPath().endsWith(".jar")) {
				IPath source = (isMuleJar(file.getName())) ? sourcePath : null;
				IClasspathAttribute[] attrs = (isMuleJar(file.getName())) ? muleAttrs
						: new IClasspathAttribute[0];
				IClasspathEntry entry = JavaCore.newLibraryEntry(new Path(file.getPath()), source,
						null, new IAccessRule[0], attrs, false);
				entries.add(entry);
			}
		}

		// Add the core library to the classpath.
		IClasspathEntry entry = getMuleCoreLibraryEntry();
		if (entry != null) {
			entries.add(entry);
		}
		return (IClasspathEntry[]) entries.toArray(new IClasspathEntry[entries.size()]);
	}

	/**
	 * Checks whether a jar is the main Mule jar.
	 * 
	 * @param fileName
	 * @return
	 */
	protected static boolean isMuleJar(String fileName) {
		if ((fileName.startsWith("mule-")) && (fileName.endsWith(".jar"))) {
			return true;
		}
		return false;
	}
}