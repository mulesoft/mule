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

package org.mule.ide.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Plugin;
import org.mule.ide.core.nature.MuleConfigNature;
import org.mule.ide.core.preferences.PreferenceConstants;

import java.io.File;

/**
 * @author Jesper
 *
 */
public class MuleCorePlugin extends Plugin {

	static private MuleCorePlugin defaultPlugin = null; 
	/**
	 * 
	 */
	public MuleCorePlugin() {
		super();
		defaultPlugin = this;
	}

	/**
	 * @return The singleton instance of the MuleCorePlugin
	 */
	static public MuleCorePlugin getDefault() {
		return defaultPlugin;
	}
	
	/**
	 * 
	 * @return The path of base of Mule executables, or null 
	 */
	public File getMulePath() {
		String path = getPluginPreferences().getString(PreferenceConstants.P_MULEPATH);
		if (path.length() < 2) return null;
		return new File(path);		
	}

	/**
	 * Sets or clears the Mule UMO Configuration nature to this project 
	 * @param project The project to set.
	 * @param setIt True if the nature should be added, false if it should be removed
	 * @throws CoreException If something goes wrong 
	 */
	public void setMuleNature(IProject project, boolean setIt) throws CoreException  {
		/*
		 * Four possible outcomes:
		 * A - transition to on
		 * B - already on
		 * C - transition to off
		 * D - already off
		 */
		if (project == null) return;
				
		IProjectDescription description = project.getDescription();
		String[] natures = description.getNatureIds();

		for (int i = 0; i < natures.length; ++i) {
			if (MuleConfigNature.NATURE_ID.equals(natures[i])) {
				if (setIt) return; // outcome B - Already had the nature

				// Outcome C - Remove the nature
				String[] newNatures = new String[natures.length - 1];
				System.arraycopy(natures, 0, newNatures, 0, i);
				System.arraycopy(natures, i + 1, newNatures, i,
						natures.length - i - 1);
				description.setNatureIds(newNatures);
				project.setDescription(description, null);
				return; // Outcome C - No longer has the nature
			}
		}
		if (! setIt) return; // Outcome D - didn't have it, just do nothing
		
		// Outcome A - add the nature
		String[] newNatures = new String[natures.length + 1];
		System.arraycopy(natures, 0, newNatures, 0, natures.length);
		newNatures[natures.length] = MuleConfigNature.NATURE_ID;
		description.setNatureIds(newNatures);
		project.setDescription(description, null);
	}
}
