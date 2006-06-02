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

package org.mule.ide.launching.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.mule.ide.core.MuleCorePlugin;
import org.mule.ide.core.exception.MuleModelException;
import org.mule.ide.core.model.IMuleConfigSet;
import org.mule.ide.core.model.IMuleModel;
import org.mule.ide.launching.IMuleLaunchConfigurationConstants;
import org.mule.ide.ui.IMuleImages;
import org.mule.ide.ui.MulePlugin;
import org.mule.ide.ui.model.MuleModelLabelProvider;

/**
 * This tab appears in the LaunchConfigurationDialog for launch configurations that require
 * Java-specific launching information such as a main type and JRE.
 */
public class MuleLauncherTab extends AbstractLaunchConfigurationTab {

	/** Table viewer for selecting project */
	private TableViewer projectsTable;

	/** Table viewer for selecting the config set within a project */
	private TableViewer configSetsTable;

	/** The current project */
	private IProject currentProject;

	/** The current config set */
	private IMuleConfigSet currentConfigSet;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		GridLayout topLayout = new GridLayout();
		comp.setLayout(topLayout);
		Dialog.applyDialogFont(comp);
		setControl(comp);

		createMainMuleConfigArea(comp);
	}

	/**
	 * Create the main area for choosing Mule launch parameters.
	 * 
	 * @param parent the parent composite
	 */
	protected void createMainMuleConfigArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		GridData gd;

		// Project label.
		Label fProjLabel = new Label(composite, SWT.NONE);
		fProjLabel.setText("Mule Project:");
		gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING);
		gd.widthHint = 110;
		fProjLabel.setLayoutData(gd);

		// Create the viewer for the Mule project list.
		setProjectsTable(new TableViewer(composite, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL
				| SWT.BORDER));
		getProjectsTable().setLabelProvider(
				WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider());
		getProjectsTable().setContentProvider(new ArrayContentProvider());
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 75;
		getProjectsTable().getTable().setLayoutData(gd);
		getProjectsTable().getTable().addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				doSelect(e);
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				doSelect(e);
			}

			protected void doSelect(SelectionEvent e) {
				IProject newProject = getSelectedProject();
				updateSelectedProject(newProject);
			}
		});

		// Config set label.
		Label fConfigLabel = new Label(composite, SWT.NONE);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING);
		gd.widthHint = 110;
		fConfigLabel.setLayoutData(gd);
		fConfigLabel.setText("Configuration Set:");

		// Config set table.
		setConfigSetsTable(new TableViewer(composite, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL
				| SWT.BORDER));
		getConfigSetsTable().setLabelProvider(
				MuleModelLabelProvider.getDecoratingMuleModelLabelProvider());
		getConfigSetsTable().setContentProvider(new ArrayContentProvider());
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 75;
		getConfigSetsTable().getTable().setLayoutData(gd);
		getConfigSetsTable().getTable().addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				doSelect(e);
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				doSelect(e);
			}

			protected void doSelect(SelectionEvent e) {
				IMuleConfigSet configSet = getSelectedConfigSet();
				updateSelectedConfigSet(configSet);
			}
		});
	}

	/**
	 * Refresh the list of projects shown in the dropdown.
	 */
	protected void refreshProjectList() {
		List result = new ArrayList();
		IProject[] projects = MuleCorePlugin.getDefault().getMuleProjects();
		for (int i = 0; i < projects.length; i++) {
			result.add(projects[i]);
		}
		getProjectsTable().setInput(result);
	}

	/**
	 * Get the selected project from the project list.
	 * 
	 * @return the selected project or null if none is selected
	 */
	protected IProject getSelectedProject() {
		IStructuredSelection selection = (IStructuredSelection) getProjectsTable().getSelection();
		if (selection.isEmpty()) {
			return null;
		}
		return (IProject) selection.getFirstElement();
	}

	/**
	 * Updates the selected project if it really changed. Also updates the list of config sets from
	 * the new project.
	 * 
	 * @param selectedProject the selected project
	 * @return true if the project changed, false if not
	 */
	protected void updateSelectedProject(IProject selectedProject) {
		if (getCurrentProject() != selectedProject) {
			setCurrentProject(selectedProject);
			if (selectedProject == null) {
				getConfigSetsTable().setInput(null);
			} else {
				try {
					IMuleModel model = MuleCorePlugin.getDefault().getMuleModel(selectedProject);
					getConfigSetsTable().setInput(model.getMuleConfigSets());
				} catch (MuleModelException e) {
					MuleCorePlugin.getDefault().getLog().log(e.getStatus());
				}
			}
			setDirty(true);
			updateLaunchConfigurationDialog();
		}
	}

	/**
	 * Get the selected config set from the config sets list.
	 * 
	 * @return the selected config set or null if none is selected.
	 */
	protected IMuleConfigSet getSelectedConfigSet() {
		IStructuredSelection selection = (IStructuredSelection) getConfigSetsTable().getSelection();
		if (selection.isEmpty()) {
			return null;
		}
		return (IMuleConfigSet) selection.getFirstElement();
	}

	/**
	 * Updates the current config set if it really changed.
	 * 
	 * @param selectedConfigSet the selected config set
	 */
	protected void updateSelectedConfigSet(IMuleConfigSet selectedConfigSet) {
		if ((getCurrentConfigSet() == null) || (getCurrentConfigSet() != selectedConfigSet)) {
			setCurrentConfigSet(selectedConfigSet);
			setDirty(true);
			updateLaunchConfigurationDialog();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration config) {

		try {
			refreshProjectList();
			String projectName = config.getAttribute(
					IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
			if (projectName.length() > 0) {
				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
				if ((project != null) && (project.isAccessible())) {
					updateSelectedProject(project);
					getProjectsTable().setSelection(new StructuredSelection(project));
					String configSetId = config.getAttribute(
							IMuleLaunchConfigurationConstants.ATTR_MULE_CONFIG_SET_ID, "");
					IMuleModel model = MuleCorePlugin.getDefault().getMuleModel(project);

					// If a config set is chosen, select it.
					if (configSetId.length() > 0) {
						IMuleConfigSet configSet = model.getMuleConfigSet(configSetId);
						if (configSet != null) {
							updateSelectedConfigSet(configSet);
							getConfigSetsTable().setSelection(new StructuredSelection(configSet));
						}
					}
					// If no config set is chosen, select the first.
					else {
						Iterator it = model.getMuleConfigSets().iterator();
						if (it.hasNext()) {
							getConfigSetsTable().setSelection(new StructuredSelection(it.next()));
						}
					}
				}
			}
		} catch (CoreException e) {
			MuleCorePlugin.getDefault().logException("Unable to initialize from launch config.", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		// Save the project choice.
		IProject project = getCurrentProject();
		if (project == null) {
			config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String) null);
		} else {
			config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project
					.getName());
		}

		// Save the config set choice.
		IMuleConfigSet configSet = getCurrentConfigSet();
		if (configSet != null) {
			config.setAttribute(IMuleLaunchConfigurationConstants.ATTR_MULE_CONFIG_SET_ID,
					configSet.getId());
		} else {
			config.setAttribute(IMuleLaunchConfigurationConstants.ATTR_MULE_CONFIG_SET_ID,
					(String) null);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
	 */
	public Image getImage() {
		return MulePlugin.getDefault().getImage(IMuleImages.KEY_MULE_LOGO);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public boolean isValid(ILaunchConfiguration config) {
		setErrorMessage(null);
		setMessage(null);

		// Make sure there is at least one Mule project to choose from.
		if (getProjectsTable().getTable().getItemCount() == 0) {
			setErrorMessage("There are no Mule projects in the workspace.");
			return false;
		}

		// Make sure there is at least one Mule project to choose from.
		if (getConfigSetsTable().getTable().getItemCount() == 0) {
			setErrorMessage("The selected Mule project has no configuration sets defined.");
			return false;
		}

		// Make sure a project was selected.
		if (getSelectedProject() == null) {
			setErrorMessage("Select a Mule project that contains the configuration set to launch.");
			return false;
		}

		// Make sure a config set was selected.
		if (getSelectedConfigSet() == null) {
			setErrorMessage("Select a configuration set to launch.");
			return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		initializeProject(config);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return "Mule";
	}

	/**
	 * If a resource element is selected in the IDE, return the project associated with it.
	 * 
	 * @return the project or null if selection can not be determined
	 */
	protected IProject getProjectForSelection() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window == null) {
			return null;
		}

		IWorkbenchPage page = window.getActivePage();
		if (page == null) {
			return null;
		}

		// Get the selection fom the active page in the workbench.
		ISelection selection = page.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) selection;
			if (!ss.isEmpty()) {
				Object obj = ss.getFirstElement();
				if (obj instanceof IResource) {
					IResource aResource = (IResource) obj;
					return aResource.getProject();
				}
			}
		}

		// Fall back to the input source of the active editor.
		IEditorPart part = page.getActiveEditor();
		if (part != null) {
			IEditorInput input = part.getEditorInput();
			IResource aResource = (IResource) input.getAdapter(IResource.class);
			if (aResource != null) {
				return aResource.getProject();
			}
		}

		return null;
	}

	/**
	 * Set the Java project attribute based on the current selection.
	 */
	protected void initializeProject(ILaunchConfigurationWorkingCopy config) {
		IProject project = getProjectForSelection();
		if ((project != null) && (project.isAccessible())) {
			config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project
					.getName());
		}
	}

	protected IMuleConfigSet getCurrentConfigSet() {
		return currentConfigSet;
	}

	protected void setCurrentConfigSet(IMuleConfigSet currentConfigSet) {
		this.currentConfigSet = currentConfigSet;
	}

	protected IProject getCurrentProject() {
		return currentProject;
	}

	protected void setCurrentProject(IProject currentProject) {
		this.currentProject = currentProject;
	}

	protected void setProjectsTable(TableViewer projectsTable) {
		this.projectsTable = projectsTable;
	}

	protected TableViewer getProjectsTable() {
		return projectsTable;
	}

	protected void setConfigSetsTable(TableViewer configSetsTable) {
		this.configSetsTable = configSetsTable;
	}

	protected TableViewer getConfigSetsTable() {
		return configSetsTable;
	}
}