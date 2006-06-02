/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Jesper Steen Møller. All rights reserved.
 * http://www.selskabet.org/jesper/
 * 
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.ide.launching;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.mule.ide.core.MuleCorePlugin;
import org.mule.ide.core.model.IMuleConfigSet;
import org.mule.ide.core.model.IMuleModel;

public class MuleLaunchConfigurationDelegate extends JavaLaunchDelegate {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate#getProgramArguments(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public String getProgramArguments(ILaunchConfiguration configuration) throws CoreException {

		// Load the attributes from the launch configuration.
		String configSetId = configuration.getAttribute(
				IMuleLaunchConfigurationConstants.ATTR_MULE_CONFIG_SET_ID, (String) null);
		String projectName = configuration.getAttribute(
				IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String) null);
		if ((configSetId == null) || (projectName == null)) {
			abort("A valid Mule project and config set are required for launch", null, 0);
		}

		// Load the project from the workspace.
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		if (project == null) {
			abort("Project '" + projectName + "' not found.", null, 0);
		}

		// Get a handle for the Mule model for the project.
		IMuleModel muleModel = MuleCorePlugin.getDefault().getMuleModel(project);
		if (muleModel == null) {
			abort("Mule model not available for project '" + projectName + "'.", null, 0);
		}

		// Find the config set by id.
		IMuleConfigSet configSet = muleModel.getMuleConfigSet(configSetId);
		if (configSet == null) {
			abort("Configuration set '" + configSetId + "' not available for project '"
					+ projectName + "'.", null, 0);
		}

		// Build the argument string.
		IPath[] paths = configSet.getConfigFilePaths();
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < paths.length; i++) {
			if (paths[i] != null) {
				if (i > 0) {
					buffer.append(",");
				}
				buffer.append(paths[i].toString());
			}
		}
		buffer.append(" 12345");
		return buffer.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate#getMainTypeName(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public String getMainTypeName(ILaunchConfiguration configuration) throws CoreException {
		return configuration.getAttribute(IMuleLaunchConfigurationConstants.ATTR_MULE_EXEC_CLASS,
				IMuleLaunchConfigurationConstants.DEFAULT_MULE_EXEC_CLASS);
	}
}