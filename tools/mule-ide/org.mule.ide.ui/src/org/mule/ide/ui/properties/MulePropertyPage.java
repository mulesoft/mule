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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.dialogs.PropertyPage;
import org.mule.ide.core.MuleCorePlugin;
import org.mule.ide.core.model.IMuleModel;
import org.mule.ide.core.nature.MuleConfigNature;
import org.mule.ide.ui.model.MuleModelContentProvider;
import org.mule.ide.ui.model.MuleModelLabelProvider;

public class MulePropertyPage extends PropertyPage {

	/** Text shown by the nature checkbox */
	private static final String NATURE_TITLE = "&Mule UMO project";

	/** Nature checkbox widget */
	private Button hasNatureButton;

	public MulePropertyPage() {
		super();
	}

	private void addNatureCheckboxSection(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		hasNatureButton = new Button(composite, SWT.CHECK);
		hasNatureButton.setText(NATURE_TITLE);
		hasNatureButton.setSelection(hasMuleNature());
		hasNatureButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}

	protected void createTabSection(Composite parent) {
		IMuleModel model = getMuleModel();
		if (model != null) {
			addSeparator(parent);
			TabFolder tabs = new TabFolder(parent, SWT.NULL);
			tabs.setLayoutData(new GridData(GridData.FILL_BOTH));
			TabItem configsTab = new TabItem(tabs, SWT.NULL);
			configsTab.setControl(createConfigurationsSection(model, tabs));
			configsTab.setText("Config Files");
			TabItem configSetsTab = new TabItem(tabs, SWT.NULL);
			configSetsTab.setControl(createConfigSetsSection(model, tabs));
			configSetsTab.setText("Config Sets");
		}
	}

	protected Composite createConfigurationsSection(IMuleModel model,
			Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout mainLayout = new GridLayout();
		mainLayout.numColumns = 2;
		composite.setLayout(mainLayout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		TableViewer viewer = new TableViewer(composite);
		viewer.setLabelProvider(MuleModelLabelProvider
				.getDecoratingMuleModelLabelProvider());
		viewer.setContentProvider(new MuleModelContentProvider(true, false));
		viewer.setInput(model);
		viewer.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));

		Composite buttons = new Composite(composite, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 10;
		layout.marginWidth = 10;
		layout.marginHeight = 10;
		buttons.setLayout(new GridLayout());
		GridData data = new GridData(GridData.VERTICAL_ALIGN_FILL
				| GridData.HORIZONTAL_ALIGN_END);
		buttons.setLayoutData(data);
		createSideButton("Add", buttons);
		createSideButton("Delete", buttons);

		return composite;
	}

	protected Composite createConfigSetsSection(IMuleModel model,
			Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		TableViewer viewer = new TableViewer(composite);
		viewer.setLabelProvider(MuleModelLabelProvider
				.getDecoratingMuleModelLabelProvider());
		viewer.setContentProvider(new MuleModelContentProvider(false, true));
		viewer.setInput(model);
		viewer.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));

		return composite;
	}

	protected Button createSideButton(String label, Composite parent) {
		Button button = new Button(parent, SWT.FLAT);
		button.setText(label);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 100;
		button.setLayoutData(data);
		return button;
	}

	/**
	 * Add a separator to the composite.
	 * 
	 * @param parent
	 *            the parent composite
	 */
	private void addSeparator(Composite parent) {
		Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		addNatureCheckboxSection(composite);
		createTabSection(composite);
		return composite;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		hasNatureButton.setSelection(hasMuleNature());
	}

	public boolean hasMuleNature() {
		try {
			IProject project = getProject(getElement());
			if (project == null)
				return false;
			return project.hasNature(MuleConfigNature.NATURE_ID);
		} catch (CoreException e) {
			MuleCorePlugin.getDefault().logException(
					"Can't determine nature of project", e);
			return false;
		}
	}

	protected IProject getProject(IAdaptable element) {
		if (element instanceof IProject) {
			return (IProject) element;
		} else {
			return (IProject) (element.getAdapter(IProject.class));
		}
	}

	/**
	 * Sets or clears the Mule UMO Configuration nature to this project
	 * 
	 * @param setIt
	 *            True if the nature should be added, false if it should be
	 *            removed
	 * @throws CoreException
	 */
	public void setMuleNature(boolean setIt) {

		try {
			IProject project = getProject(getElement());
			if (project == null)
				return;
			MuleCorePlugin.getDefault().setMuleNature(project, setIt);
		} catch (CoreException e) {
			MuleCorePlugin.getDefault().logException(
					"Can't set the project description", e);
		}
	}

	/**
	 * Get the Mule IDE model for the current project.
	 * 
	 * @return the model
	 */
	protected IMuleModel getMuleModel() {
		return MuleCorePlugin.getDefault().getMuleModel(
				getProject(getElement()));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferencePage#performOk()
	 */
	public boolean performOk() {
		setMuleNature(hasNatureButton.getSelection());
		return true;
	}
}