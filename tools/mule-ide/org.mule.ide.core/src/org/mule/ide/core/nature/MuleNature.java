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

package org.mule.ide.core.nature;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.ecore.resource.Resource;
import org.mule.ide.DocumentRoot;
import org.mule.ide.MuleIDEFactory;
import org.mule.ide.MuleIdeConfigType;
import org.mule.ide.core.IMuleDefaults;
import org.mule.ide.core.MuleCorePlugin;
import org.mule.ide.core.builder.MuleConfigBuilder;
import org.mule.ide.core.model.IMuleModel;
import org.mule.ide.internal.core.model.MuleModel;
import org.mule.ide.internal.core.model.MuleModelDeltaListener;
import org.mule.ide.util.MuleIDEResourceFactoryImpl;

public class MuleNature implements IProjectNature {

	/** The parent project */
	private IProject project;

	/** The mule model for this project */
	private IMuleModel muleModel;

	/** Error message for marker when config file can not be created */
	private static final String ERROR_CREATING_CONFIG_FILE = "Could not create Mule IDE configuration file.";

	/** Name for folder linked to the external Mule install */
	public static final String EXTERNAL_LIBS_LINK_DIR = ".mulelibs";

	/** ID of this project nature */
	public static final String NATURE_ID = "org.mule.ide.core.muleConfigNature";

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IProjectNature#configure()
	 */
	public void configure() throws CoreException {
		IProjectDescription desc = project.getDescription();
		ICommand[] commands = desc.getBuildSpec();

		for (int i = 0; i < commands.length; ++i) {
			if (commands[i].getBuilderName().equals(MuleConfigBuilder.BUILDER_ID)) {
				return;
			}
		}

		ICommand[] newCommands = new ICommand[commands.length + 1];
		System.arraycopy(commands, 0, newCommands, 0, commands.length);
		ICommand command = desc.newCommand();
		command.setBuilderName(MuleConfigBuilder.BUILDER_ID);
		newCommands[newCommands.length - 1] = command;
		desc.setBuildSpec(newCommands);
		project.setDescription(desc, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IProjectNature#deconfigure()
	 */
	public void deconfigure() throws CoreException {
		IProjectDescription description = getProject().getDescription();
		ICommand[] commands = description.getBuildSpec();
		for (int i = 0; i < commands.length; ++i) {
			if (commands[i].getBuilderName().equals(MuleConfigBuilder.BUILDER_ID)) {
				ICommand[] newCommands = new ICommand[commands.length - 1];
				System.arraycopy(commands, 0, newCommands, 0, i);
				System.arraycopy(commands, i + 1, newCommands, i, commands.length - i - 1);
				description.setBuildSpec(newCommands);
				return;
			}
		}
	}

	/**
	 * Get the Mule IDE model (lazy init)
	 * 
	 * @return the model
	 */
	public IMuleModel getMuleModel() {
		if (muleModel == null) {
			muleModel = new MuleModel(getProject());
			IStatus status = muleModel.refresh();

			if (!status.isOK()) {
				IFile file = getProject().getFile(IMuleDefaults.MULE_IDE_CONFIG_FILENAME);
				if (file.exists()) {
					MuleCorePlugin.getDefault().clearMarkers(file);
					MuleCorePlugin.getDefault().createMarker(file, IMarker.SEVERITY_ERROR,
							status.getMessage());
				}
			}
			ResourcesPlugin.getWorkspace().addResourceChangeListener(
					new MuleModelDeltaListener(muleModel));
		}
		return muleModel;
	}

	/**
	 * If the Mule IDE config file does not exist, create it.
	 */
	protected void createConfigFileIfNeeded() {
		IFile file = getProject().getFile(IMuleDefaults.MULE_IDE_CONFIG_FILENAME);
		if (!file.exists()) {
			try {
				Resource resource = (new MuleIDEResourceFactoryImpl()).createResource(null);
				DocumentRoot root = MuleIDEFactory.eINSTANCE.createDocumentRoot();
				MuleIdeConfigType config = MuleIDEFactory.eINSTANCE.createMuleIdeConfigType();
				root.setMuleIdeConfig(config);
				resource.getContents().add(root);
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				resource.save(output, Collections.EMPTY_MAP);
				ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
				file.create(input, true, new NullProgressMonitor());
			} catch (Exception e) {
				MuleCorePlugin.getDefault().createMarker(getProject(), IMarker.SEVERITY_ERROR,
						ERROR_CREATING_CONFIG_FILE);
				MuleCorePlugin.getDefault().logException(ERROR_CREATING_CONFIG_FILE, e);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IProjectNature#getProject()
	 */
	public IProject getProject() {
		return project;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IProjectNature#setProject(org.eclipse.core.resources.IProject)
	 */
	public void setProject(IProject project) {
		this.project = project;
		createConfigFileIfNeeded();
	}
}