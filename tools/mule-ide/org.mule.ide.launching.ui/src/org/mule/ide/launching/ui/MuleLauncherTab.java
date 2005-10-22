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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.SWTUtil;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.mule.ide.core.MuleCorePlugin;
import org.mule.ide.core.builder.MuleConfigBuilder;
import org.mule.ide.core.nature.MuleConfigNature;
import org.mule.ide.launching.IMuleConfigLaunchConfigurationConstants;

/**
 * This tab appears in the LaunchConfigurationDialog for launch configurations that
 * require Java-specific launching information such as a main type and JRE.
 */
public class MuleLauncherTab extends AbstractLaunchConfigurationTab {

	private static final String MULE_CONFIG_FILE_SUFFIX = ".xml";

	// Tab general info
	private final Image fMuleIcon = createLauncherlImage("icons/mule16.gif"); //$NON-NLS-1$

	// Project UI widgets
	private Label fProjLabel;
	private Text fProjText;
	private Button fProjButton;
	
	// File to run UI widgets
	private Label fConfigLabel;
	private Text fConfigText;
	private Button fConfigChooseButton;
	
	private MuleConfigBuilder configBuilder = new MuleConfigBuilder();
	
	/**
	 * @see ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {		
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);

		GridLayout topLayout = new GridLayout();
		topLayout.numColumns= 3;
		comp.setLayout(topLayout);		
		
		Label label = new Label(comp, SWT.NONE);
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		label.setLayoutData(gd);
		
		createSingleTestSection(comp);
		
		label = new Label(comp, SWT.NONE);
		gd = new GridData();
		gd.horizontalSpan = 3;
		label.setLayoutData(gd);
		
		Dialog.applyDialogFont(comp);
		
		validatePage();
	}

	protected void createSingleTestSection(Composite comp) {
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		
		fProjLabel = new Label(comp, SWT.NONE);
		fProjLabel.setText("Project"); 
		gd= new GridData();
		gd.horizontalAlignment |= GridData.HORIZONTAL_ALIGN_END;
		fProjLabel.setLayoutData(gd);
		gd.horizontalAlignment &= ~GridData.HORIZONTAL_ALIGN_END;
		
		fProjText = new Text(comp, SWT.SINGLE | SWT.BORDER);
		fProjText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fProjText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				validatePage();
				updateLaunchConfigurationDialog();				
				fConfigChooseButton.setEnabled(fProjText.getText().length() > 0);
			}
		});
			
		fProjButton = new Button(comp, SWT.PUSH);
		fProjButton.setText("..."); 
		fProjButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleProjectButtonSelected();
			}
		});
		setButtonGridData(fProjButton);
		
		fConfigLabel = new Label(comp, SWT.NONE);
		gd = new GridData();
		gd.horizontalAlignment |= GridData.HORIZONTAL_ALIGN_END;
		fConfigLabel.setLayoutData(gd);
		gd.horizontalAlignment &= ~GridData.HORIZONTAL_ALIGN_END;
		fConfigLabel.setText("Configuration File"); 
			
		fConfigText = new Text(comp, SWT.SINGLE | SWT.BORDER);
		fConfigText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fConfigText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				validatePage();
				updateLaunchConfigurationDialog();
			}
		});
		
		fConfigChooseButton = new Button(comp, SWT.PUSH);
		fConfigChooseButton.setEnabled(fProjText.getText().length() > 0);		
		fConfigChooseButton.setText("..."); 
		fConfigChooseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleSearchButtonSelected();
			}
		});
		setButtonGridData(fConfigChooseButton);
		
		new Label(comp, SWT.NONE);
	}

	protected static Image createLauncherlImage(String path) {
		ImageDescriptor id= ImageDescriptor.createFromFile(MuleLauncherTab.class, path);
		return id.createImage();
	}

	/**
	 * @see ILaunchConfigurationTab#initializeFrom(ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration config) {

		// Update project
		String projectName= ""; //$NON-NLS-1$
		try {
			projectName = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, ""); //$NON-NLS-1$
		} catch (CoreException ce) {
		}
		fProjText.setText(projectName);
		
		// Update config name
		String configName= ""; //$NON-NLS-1$
		try {
			configName = config.getAttribute(IMuleConfigLaunchConfigurationConstants.ATTR_CONFIG_FILE_NAME, ""); //$NON-NLS-1$
		} catch (CoreException ce) {			
		}
		fConfigText.setText(configName);
	}

	/*
	 * @see ILaunchConfigurationTab#performApply(ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, fProjText.getText());
		config.setAttribute(IMuleConfigLaunchConfigurationConstants.ATTR_CONFIG_FILE_NAME, fConfigText.getText());
	}

	/*
	 * @see ILaunchConfigurationTab#dispose()
	 */
	public void dispose() {
		super.dispose();
		fMuleIcon.dispose();
	}

	/*
	 * @see AbstractLaunchConfigurationTab#getImage()
	 */
	public Image getImage() {
		return fMuleIcon;
	}

	void collect(IContainer inside, String suffix, List result) throws CoreException {
		if (inside == null) return;
		
		IResource[] resources = inside.members();
		for (int i=0; i<resources.length; ++i) {
			if (resources[i] instanceof IContainer)
				collect((IContainer)resources[i], suffix, result);
			else if (resources[i] instanceof IFile) {
				if (resources[i].getName().endsWith(suffix)) result.add(resources[i]);
			}
		}
	}
	
	/**
	 * Show a dialog that lists all main types
	 */
	protected IResource chooseConfigFile() {
		Shell shell = getShell();
		
		IProject project = getProject();
		if (project == null) {
			MessageDialog.openError(shell, "Choose Mule Config File", "No project selected");
			return null;
		}
		
		ILabelProvider labelProvider= new WorkbenchLabelProvider() {
			protected String decorateText(String input, Object element) {
				if (element instanceof IFile) return input + " - " + ((IFile)element).getProjectRelativePath().toString();
				return input;				
			}
		};
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), labelProvider);
		
		dialog.setTitle("Choose Mule Config File"); 
		dialog.setMessage("Select the config file you wish to run or debug"); 
		try {
			List l = new ArrayList();
			collect(project, MULE_CONFIG_FILE_SUFFIX, l);
			if (l.size() < 1) {
				MessageDialog.openError(shell, "Mule Config File", "No Mule config files in selected project");
				return null;
			}
			
			dialog.setElements(l.toArray());
			if (dialog.open() == Window.OK) {
				IFile file = (IFile) dialog.getFirstResult();
				if (configBuilder.isCorrectXML(file)) {
					fConfigText.setText(file.getProjectRelativePath().toString());
				}
			}			
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Show a dialog that lists all main types
	 */
	protected void handleSearchButtonSelected() {
		chooseConfigFile();
	}

	
	/**
	 * Show a dialog that lets the user select a project.  This in turn provides
	 * context for the main type, allowing the user to key a main type name, or
	 * constraining the search for main types to the specified project.
	 */
	protected void handleProjectButtonSelected() {
		IProject project = chooseProject();
		if (project == null) {
			return;
		}
		
		String projectName = project.getName();
		fProjText.setText(projectName);		
	}
	
	/*
	 * Realize a Java Project selection dialog and return the first selected project,
	 * or null if there was none.
	 */
	protected IProject chooseProject() {
		IProject[] projects;
		projects= getWorkspaceRoot().getProjects();
		
		ILabelProvider labelProvider = new WorkbenchLabelProvider();
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), labelProvider);
		dialog.setTitle("Choose Project"); 
		dialog.setMessage("Select the project containing the classes you wish to launch a Mule configuration for."); 
		dialog.setElements(projects);
		
		IProject aProject = getProject();
		if (aProject != null) {
			dialog.setInitialSelections(new Object[] { aProject });
		}
		if (dialog.open() == Window.OK) {			
			IProject project = (IProject) dialog.getFirstResult();
			try {
				if (! project.hasNature(MuleConfigNature.NATURE_ID)) {
					if (MessageDialog.openQuestion(getShell(), "Choose project", 
							"The selected project is not associated with Mule. Would you like to associate '" + project.getName() + "' with Mule?")) {
						MuleCorePlugin.getDefault().setMuleNature(project, true);
					}
				}
			} catch (CoreException e) {
				MessageDialog.openError(getShell(), "Set project nature", "Unable to set project nature: " + e.getLocalizedMessage());
			}
			return project;
		}
		return null;
	}
	
	/*
	 * Return the IProject corresponding to the project name in the project name
	 * text field, or null if the text does not match a project name.
	 */
	protected IProject getProject() {
		String projectName = fProjText.getText().trim();
		if (projectName.length() < 1) {
			return null;
		}
		return getWorkspaceRoot().getProject(projectName);		
	}
	
	/*
	 * Convenience method to get the workspace root.
	 */
	private IWorkspaceRoot getWorkspaceRoot() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}
	
	/*
	 * @see ILaunchConfigurationTab#isValid(ILaunchConfiguration)
	 */
	public boolean isValid(ILaunchConfiguration config) {
		return getErrorMessage() == null;
	}
	
	private void validatePage() {
		setErrorMessage(null);
		setMessage(null);
		
		File muleRoot = MuleCorePlugin.getDefault().getMulePath();
		if (muleRoot == null)  {
			setErrorMessage("Mule is not set up (see Preferences...)");
			return;
		}
		
		String projectName = fProjText.getText().trim();
		if (projectName.length() == 0) {
			setErrorMessage("No project selected");
			return;
		}

		IProject project = getWorkspaceRoot().getProject(projectName);
		if (!project.exists()) {
			setErrorMessage("Project does not exist"); 
			return;
		}
		
		try {
			if (!project.hasNature(MuleConfigNature.NATURE_ID)) {
 				setErrorMessage("The selected project is not a Mule project"); 
			}
			
			if (!project.hasNature(JavaCore.NATURE_ID)) {
 				setMessage("The selected project is not a Java project"); 
			}
			
			String configPath = fConfigText.getText().trim();
			if (configPath.length() == 0) {
				setErrorMessage("No Mule Configuration selected"); 
				return;
			}
			IFile theFile = project.getFile(configPath);
			if (! theFile.exists()) {
				setErrorMessage("Mule Configuration does not exists"); 
				return;
			}
		} catch (Exception e) {
		}
	}

	/*
	 * @see ILaunchConfigurationTab#setDefaults(ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		IResource element = getContextFile();
		if (element != null) {
			initializeProject(element, config);
		} else {
			// We set empty attributes for project & main type so that when one config is
			// compared to another, the existence of empty attributes doesn't cause an
			// incorrect result (the performApply() method can result in empty values
			// for these attributes being set on a config if there is nothing in the
			// corresponding text boxes)
			config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, ""); //$NON-NLS-1$
			config.setAttribute(IMuleConfigLaunchConfigurationConstants.ATTR_CONFIG_FILE_NAME, ""); //$NON-NLS-1$
		}
		initializeTestAttributes(getContextFlowFile(), config);
	}
	
	/*
	 * @see ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return "Mule"; 
	}

	protected void setButtonGridData(Button button) {
		GridData gridData= new GridData();
		button.setLayoutData(gridData);
		SWTUtil.setButtonDimensionHint(button);
	}

	/**
	 * Returns the current file resource from which to initialize
	 * default settings, or <code>null</code> if none.
	 * 
	 * @return File context.
	 */
	protected IResource getContextFile() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window == null)
			return null;
		IWorkbenchPage page = window.getActivePage();
				
		if (page == null) return null;
		ISelection selection = page.getSelection();
		
		IResource aFile = null;
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection)selection;
			if (!ss.isEmpty()) {
				Object obj = ss.getFirstElement();
				if (obj instanceof IResource) {
					aFile = (IResource)obj;
				}
			}
		}
		IEditorPart part = page.getActiveEditor();
		if (part != null) {
			IEditorInput input = part.getEditorInput();
			aFile = (IResource)input.getAdapter(IResource.class);
		}
		
		return aFile;
	}

	/**
	 * Returns the current file resource from which to initialize
	 * default settings, or <code>null</code> if none.
	 * 
	 * @return File context.
	 */
	protected IFile getContextFlowFile() {
		IResource aRes = getContextFile();
		if (aRes instanceof IFile) {
			IFile aFile = (IFile)aRes;
			if (configBuilder.isCorrectXML(aFile)) return aFile;
		}
		return null;
	}
	
	/**
	 * Set the java project attribute based on the IJavaElement.
	 */
	protected void initializeProject(IResource element, ILaunchConfigurationWorkingCopy config) {
		IProject project = element.getProject();
		String name = null;
		if (project != null && project.exists()) {
			name = project.getName();
		}
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, name);
	}
	
	private void initializeTestAttributes(IFile flowFile, ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IMuleConfigLaunchConfigurationConstants.ATTR_CONFIG_FILE_NAME, flowFile != null ? flowFile.getProjectRelativePath().toString() : "");
		initializeName(config, flowFile != null ? flowFile.getName() : null);
	}
	
	private void initializeName(ILaunchConfigurationWorkingCopy config, String name) {
		if (name == null) {
			name= ""; //$NON-NLS-1$
		}
		if (name.length() > 0) {
			int index = name.lastIndexOf('.');
			if (index > 0) {
				name = name.substring(0, index);
			}
			name= getLaunchConfigurationDialog().generateName(name);
			config.rename(name);
		}
	}
}
