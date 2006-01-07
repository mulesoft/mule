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

package org.mule.ide.ui.preferences;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.mule.ide.core.MuleCorePlugin;
import org.mule.ide.core.exception.MuleModelException;
import org.mule.ide.core.nature.MuleConfigNature;
import org.mule.ide.core.preferences.IPreferenceConstants;
import org.mule.ide.ui.MulePlugin;

/**
 * Preference page for Mule setttings.
 */
public class MulePreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	/** The radio button group for classpath type */
	private RadioGroupFieldEditor classpathType;

	/** Field editor for external Mule root directory */
	private DirectoryFieldEditor externalMuleRoot;

	/** Holds the initial value of the classpath type */
	private String initialClasspathType;

	/** Holds the initial value of the external mule root folder */
	private String initialExternalMuleRoot;

	public MulePreferencePage() {
		super(GRID);
		setPreferenceStore(MulePlugin.getDefault().getPreferenceStore());
		setDescription("Default settings for Mule UMO projects");
		updateInitialValues();
	}

	/**
	 * Update the initial values.
	 */
	protected void updateInitialValues() {
		setInitialClasspathType(getPreferenceStore().getString(
				IPreferenceConstants.MULE_CLASSPATH_TYPE));
		setInitialExternalMuleRoot(getPreferenceStore().getString(
				IPreferenceConstants.EXTERNAL_MULE_ROOT));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
	 */
	public void createFieldEditors() {

		classpathType = new RadioGroupFieldEditor(IPreferenceConstants.MULE_CLASSPATH_TYPE,
				"Choose where Eclipse will look for Mule libraries:", 1, new String[][] {
						{ "Mule plugin (jars included with Mini-Mule distribution)",
								IPreferenceConstants.MULE_CLASSPATH_TYPE_PLUGIN },
						{ "External location specified below",
								IPreferenceConstants.MULE_CLASSPATH_TYPE_EXTERNAL } },
				getFieldEditorParent(), true);
		addField(classpathType);
		externalMuleRoot = new DirectoryFieldEditor(IPreferenceConstants.EXTERNAL_MULE_ROOT,
				"&Mule installation folder:", getFieldEditorParent());
		addField(externalMuleRoot);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.PreferencePage#performApply()
	 */
	protected void performApply() {
		super.performApply();
		updateInitialValues();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#performOk()
	 */
	public boolean performOk() {
		boolean ok = super.performOk();
		if (ok) {
			// Handle external Mule root change.
			String newExternalMuleRoot = getPreferenceStore().getString(
					IPreferenceConstants.EXTERNAL_MULE_ROOT);
			boolean externalMuleRootChanged = ((getInitialExternalMuleRoot() == null) || (!getInitialExternalMuleRoot()
					.equals(newExternalMuleRoot)));
			if (externalMuleRootChanged) {
				try {
					MuleCorePlugin.getDefault().updateExternalMuleRootVariable();
				} catch (MuleModelException e) {
					MulePlugin.getDefault().showError(e.getMessage(), e.getStatus());
				}
			}

			// Handle classpath type change.
			String newClasspathType = getPreferenceStore().getString(
					IPreferenceConstants.MULE_CLASSPATH_TYPE);
			boolean classpathTypeChanged = ((getInitialClasspathType() == null) || (!getInitialClasspathType()
					.equals(newClasspathType)));
			if (classpathTypeChanged) {
				updateClasspathForMuleProjects();
			}
		}
		return ok;
	}

	/**
	 * Updates the Mule libraries classpath for all Mule projects. This will be changing to a
	 * project setting rather than a general preference soon.
	 */
	protected void updateClasspathForMuleProjects() {
		IProject[] projects = MuleCorePlugin.getDefault().getMuleProjects();
		for (int i = 0; i < projects.length; i++) {
			MuleConfigNature config = MuleCorePlugin.getDefault().getMuleNature(projects[i]);
			try {
				config.refreshMuleClasspathContainer();
			} catch (MuleModelException e) {
				MuleCorePlugin.getDefault().getLog().log(e.getStatus());
			}
		}
	}

	protected void setInitialClasspathType(String initialClasspathType) {
		this.initialClasspathType = initialClasspathType;
	}

	protected String getInitialClasspathType() {
		return initialClasspathType;
	}

	protected void setInitialExternalMuleRoot(String initialExternalMuleRoot) {
		this.initialExternalMuleRoot = initialExternalMuleRoot;
	}

	protected String getInitialExternalMuleRoot() {
		return initialExternalMuleRoot;
	}
}