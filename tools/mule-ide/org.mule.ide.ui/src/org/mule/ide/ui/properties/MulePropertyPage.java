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

package org.mule.ide.ui.properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;
import org.mule.ide.core.MuleCorePlugin;
import org.mule.ide.core.nature.MuleConfigNature;
import org.mule.ide.ui.MulePlugin;

public class MulePropertyPage extends PropertyPage {

	private static final String PATH_TITLE = "Path:";
	private static final String NATURE_TITLE = "&Mule UMO project";

	private Button hasNatureButton;
	
	/**
	 * Constructor for SamplePropertyPage.
	 */
	public MulePropertyPage() {
		super();
	}

	private void addFirstSection(Composite parent) {
		Composite composite = createDefaultComposite(parent);

		//Label for path field
		Label pathLabel = new Label(composite, SWT.NONE);
		pathLabel.setText(PATH_TITLE);

		// Path text field
		Text pathValueText = new Text(composite, SWT.WRAP | SWT.READ_ONLY);
		pathValueText.setText(((IResource) getElement()).getFullPath().toString());
	}

	private void addSeparator(Composite parent) {
		Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		separator.setLayoutData(gridData);
	}

	private void addSecondSection(Composite parent) {
		Composite composite = createDefaultComposite(parent);

		// Owner text field
		hasNatureButton = new Button(composite, SWT.CHECK);
		hasNatureButton.setText(NATURE_TITLE);
		hasNatureButton.setSelection(hasMuleNature());
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		hasNatureButton.setLayoutData(gd);
	}

	/**
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL);
		data.grabExcessHorizontalSpace = true;
		composite.setLayoutData(data);

		addFirstSection(composite);
		addSeparator(composite);
		addSecondSection(composite);
		return composite;
	}

	private Composite createDefaultComposite(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);

		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData(data);

		return composite;
	}

	protected void performDefaults() {
		// Populate the owner text field with the default value
		hasNatureButton.setSelection(hasMuleNature());
	}
	
	public boolean hasMuleNature() {
		try {
			IProject project = getProject(getElement()); 
			if (project == null) return false;
			return project.hasNature(MuleConfigNature.NATURE_ID);
		} catch (CoreException e) {
			log("Can't determine nature of project", e);
			return false;
		}
	}

	/**
	 * Logs to the plugin's log
	 * 
	 * @param excep
	 */
	private void log(String message, CoreException excep) {
		MulePlugin.getDefault().getLog().log(new Status(IStatus.ERROR, MulePlugin.PLUGIN_ID, IStatus.OK, 
				message, excep));
	}
	
	protected IProject getProject(IAdaptable element) {
		if (element instanceof IProject) {
			return (IProject) element;
		} else  {
			return (IProject) (element.getAdapter(IProject.class));
		}
	}
	
	/**
	 * Sets or clears the Mule UMO Configuration nature to this project 
	 * @param setIt True if the nature should be added, false if it should be removed
	 * @throws CoreException
	 */
	public void setMuleNature(boolean setIt) {

		try {
			IProject project = getProject(getElement());
			if (project == null) return;
			MuleCorePlugin.getDefault().setMuleNature(project, setIt);
		} catch (CoreException e) {
			log("Can't set the project description", e);
		}
	}
	
	public boolean performOk() {
		// store the value in the owner text field
		setMuleNature(hasNatureButton.getSelection());
		return true;
	}

}