package org.mule.ide.ui.wizards;

import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

public class MuleWizardProjectPage extends WizardNewProjectCreationPage {

	/** Naming constant for project page */
	private static final String PAGE_PROJECT = "muleWizardProjectPage";

	public MuleWizardProjectPage() {
		super(PAGE_PROJECT);
		setTitle("Mule Project");
		setDescription("Create a new Mule project.");
	}
}
