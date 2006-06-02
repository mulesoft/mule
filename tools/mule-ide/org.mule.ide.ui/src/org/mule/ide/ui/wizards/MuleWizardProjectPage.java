package org.mule.ide.ui.wizards;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.mule.ide.ui.panels.MuleClasspathChooser;
import org.mule.ide.ui.panels.ProjectContentChooser;

/**
 * First page of the wizard to create a new Mule project
 * 
 * @author Derek Adams
 */
public class MuleWizardProjectPage extends WizardNewProjectCreationPage {

	/** Widgets needed to choose Mule lib locations */
	private MuleClasspathChooser classpathChooser;

	/** Widgets needed to choose the initial project content */
	private ProjectContentChooser contentChooser;

	/** Naming constant for project page */
	private static final String PAGE_PROJECT = "muleWizardProjectPage";

	public MuleWizardProjectPage() {
		super(PAGE_PROJECT);
		setTitle("Mule Project");
		setDescription("Create a new Mule project.");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.WizardNewProjectCreationPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		Composite existing = (Composite) getControl();
		classpathChooser = new MuleClasspathChooser();
		classpathChooser.createControl(existing);
		contentChooser = new ProjectContentChooser();
		contentChooser.createControl(existing);
		contentChooser.setChoice(ProjectContentChooser.LOAD_FROM_EMPTY);
	}

	/**
	 * Indicates whether the user is choosing to load libs from the core plugin.
	 * 
	 * @return
	 */
	public boolean isChoosingLibsFromPlugin() {
		return classpathChooser.getLibLocationChoice() == MuleClasspathChooser.LOAD_FROM_PLUGIN;
	}

	/**
	 * Indicates whether the user is choosing to load libs from an external location.
	 * 
	 * @return
	 */
	public boolean isChoosingLibsFromExternal() {
		return classpathChooser.getLibLocationChoice() == MuleClasspathChooser.LOAD_FROM_EXTERNAL;
	}

	/**
	 * Get the external location chosen for loading Mule libraries.
	 * 
	 * @return
	 */
	public String getExternalRoot() {
		return classpathChooser.getExternalRoot();
	}

	/**
	 * Gets the chosen sample project name or null of "empty" or none chosen.
	 * 
	 * @return the sample project description or null
	 */
	public String getSelectedSampleProject() {
		return contentChooser.getChosenSampleDescription();
	}
}