/*
 * Copyright 2005 SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 * 
 * ------------------------------------------------------------------------------------------------------
 * $Header$
 * $Revision$
 * $Date$
 */
package org.mule.jbi.management;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.jbi.util.IOUtils;

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public class Directories {

	public static final String TEMP_DIR = "temp";
	public static final String ENGINES_DIR = "engines";
	public static final String BINDINGS_DIR = "bindings";
	public static final String LIBRARIES_DIR = "libraries";
	public static final String ASSEMBLIES_DIR = "assemblies";
	public static final String INSTALL_DIR = "install";
	public static final String DEPLOY_DIR = "deploy";
	public static final String PROCESSED_DIR = "processed";
	public static final String WORKSPACE_DIR = "workspace";

	private static int counter;
	
	private static Log logger = LogFactory.getLog(Directories.class);
	
	public static synchronized File getNewTempDir(File rootDir) {
		while (true) {
			String s = Integer.toHexString(++counter);
			while (s.length() < 8) {
				s = "0" + s;
			}
			File f = new File(rootDir, File.separator + TEMP_DIR + File.separator + s);
			if (!f.exists()) {
				return f;
			}
		}
	}
	
	public static File getEngineInstallDir(File rootDir, String name) {
		return new File(rootDir, ENGINES_DIR + File.separator + validateString(name));
	}
	
	public static File getEngineWorkspaceDir(File rootDir, String name) {
		return new File(rootDir, WORKSPACE_DIR + File.separator + validateString(name));
	}
	
	public static File getBindingInstallDir(File rootDir, String name) {
		return new File(rootDir, BINDINGS_DIR + File.separator + validateString(name));
	}
	
	public static File getBindingWorkspaceDir(File rootDir, String name) {
		return new File(rootDir, WORKSPACE_DIR + File.separator + validateString(name));
	}
	
	public static File getLibraryInstallDir(File rootDir, String name) {
		return new File(rootDir, LIBRARIES_DIR + File.separator + validateString(name));
	}
	
	public static File getAssemblyInstallDir(File rootDir, String name) {
		return new File(rootDir, ASSEMBLIES_DIR + File.separator + validateString(name));
	}
	
	public static File getAutoInstallDir(File rootDir) {
		return new File(rootDir, INSTALL_DIR);
	}
	
	public static File getAutoInstallProcessedDir(File rootDir) {
		return new File(rootDir, INSTALL_DIR + File.separator + PROCESSED_DIR);
	}
	
	public static File getAutoDeployDir(File rootDir) {
		return new File(rootDir, DEPLOY_DIR);
	}
	
	public static File getAutoDeployProcessedDir(File rootDir) {
		return new File(rootDir, DEPLOY_DIR + File.separator + PROCESSED_DIR);
	}

	public static void deleteMarkedDirectories(File dir) {
    	if (dir != null && dir.isDirectory()) {
    		if (new File(dir, ".delete").isFile()) {
    			deleteDir(dir);
    		} else {
	    		File[] children = dir.listFiles();
	    		for (int i = 0; i < children.length; i++) {
	    			if (children[i].isDirectory()) {
	    				deleteMarkedDirectories(children[i]);
	    			}
				}
	    	}
    	}    	
	}
	
	public static void deleteDir(String dir) {
		deleteDir(new File(dir));
	}
	
	public static void deleteDir(File dir) {
		IOUtils.deleteFile(dir);
		if (dir.isDirectory()) {
			try {
				new File(dir, ".delete").createNewFile();
			} catch (IOException e) {
				logger.warn("Could not mark directory to be deleted", e);
			}
		}
	}
	
	public static void createDirectories(File rootDir) throws IOException {
		IOUtils.createDirs(rootDir);
		IOUtils.createDirs(new File(rootDir, ENGINES_DIR));
		IOUtils.createDirs(new File(rootDir, BINDINGS_DIR));
		IOUtils.createDirs(new File(rootDir, WORKSPACE_DIR));
		IOUtils.createDirs(new File(rootDir, LIBRARIES_DIR));
		IOUtils.createDirs(new File(rootDir, ASSEMBLIES_DIR));
		IOUtils.createDirs(getAutoInstallDir(rootDir));
		IOUtils.createDirs(getAutoDeployDir(rootDir));
		IOUtils.createDirs(getAutoDeployProcessedDir(rootDir));
	}
	
    private static String validateString(String str) {
    	str = str.replace(':', '_');
    	str = str.replace('/', '_');
    	str = str.replace('\\', '_');
    	return str;
    }

	
}
