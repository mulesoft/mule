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

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.mule.ide.core.MuleCorePlugin;
import org.mule.ide.core.exception.MuleModelException;
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

	/** The text widget in the directory field editor */
	Text textExternalMuleRoot;

	public MulePreferencePage() {
		super(GRID);
		setPreferenceStore(MulePlugin.getDefault().getPreferenceStore());
		setDescription("Default settings for Mule UMO projects");
	}

	/**
	 * Creates the field editors. Field editors are abstractions of the common GUI blocks needed to
	 * manipulate various types of preferences. Each field editor knows how to save and restore
	 * itself.
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
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#performOk()
	 */
	public boolean performOk() {
		boolean ok = super.performOk();
		if (ok) {
			try {
				MuleCorePlugin.getDefault().updateExternalMuleRootVariable();
			} catch (MuleModelException e) {
				MuleCorePlugin.getDefault().logException("Unable to set Mule root variable", e);
			}
		}
		return ok;
	}
}