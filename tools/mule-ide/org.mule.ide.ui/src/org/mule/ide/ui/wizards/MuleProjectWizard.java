package org.mule.ide.ui.wizards;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPage;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.mule.ide.core.MuleClasspathUtils;
import org.mule.ide.core.MuleCorePlugin;
import org.mule.ide.core.exception.MuleModelException;
import org.mule.ide.core.model.IMuleConfigSet;
import org.mule.ide.core.model.IMuleConfiguration;
import org.mule.ide.core.model.IMuleModel;
import org.mule.ide.core.nature.MuleNature;
import org.mule.ide.core.samples.ConfigSet;
import org.mule.ide.core.samples.Sample;
import org.mule.ide.core.samples.SampleLoader;
import org.mule.ide.ui.MulePlugin;
import org.osgi.framework.Bundle;

/**
 * Wizard for creating a new Mule project.
 */
public class MuleProjectWizard extends Wizard implements INewWizard {

	/** The workbench handle */
	private IWorkbench workbench;

	/** Page for creating a new project */
	private MuleWizardProjectPage projectPage;

	/** Page for setting up java project capabilities */
	private NewJavaProjectWizardPage javaPage;

	/** Static constant for config folder name */
	private static final String CONFIG_FOLDER_NAME = "conf";

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.IWizard#addPages()
	 */
	public void addPages() {
		projectPage = new MuleWizardProjectPage();
		addPage(projectPage);
		javaPage = new NewJavaProjectWizardPage(ResourcesPlugin.getWorkspace().getRoot(), projectPage);
		addPage(javaPage);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.IWizard#performFinish()
	 */
	public boolean performFinish() {
		try {
			// Set up the Java project according to entries on Java page.
			getContainer().run(false, true, javaPage.getRunnable());

			// Add the Mule nature.
			MuleCorePlugin.getDefault().setMuleNature(projectPage.getProjectHandle(), true);

			// Add the Mule classpath container.
			IProject project = projectPage.getProjectHandle();
			IJavaProject javaProject = JavaCore.create(project);
			addMuleLibraries(javaProject);
			String sampleChosen = projectPage.getSelectedSampleProject();
			if (sampleChosen != null) {
				Sample sample = SampleLoader.getInstance().getSampleByDescription(sampleChosen);
				addFromSample(sample, javaProject);
			} else {
				Sample sample = createEmptyProject();
				addFromSample(sample, javaProject);
			}
			return true;
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof CoreException) {
				MulePlugin.getDefault().showError("Unable to create project.",
						((CoreException) e.getCause()).getStatus());
			}
		} catch (InterruptedException e) {
		} catch (CoreException e) {
			MulePlugin.getDefault().showError("Unable to create project.", e.getStatus());
		}
		return false;
	}

	/**
	 * A sample that creates the content for an empty project
	 * 
	 * @return the sample
	 */
	protected Sample createEmptyProject() {
		Sample sample = new Sample();
		sample.setPluginId(MulePlugin.PLUGIN_ID);
		sample.setDescription("Empty Project");
		sample.setRoot("empty");
		sample.setSourcePath("src");
		sample.setConfigPath("conf");
		ConfigSet set = new ConfigSet();
		set.setName("Default");
		set.setConfigPath("mule-config.xml");
		set.setConfigName("Default Mule Config");
		sample.setConfigSets(new ConfigSet[] { set });
		return sample;
	}

	/**
	 * Add the Mule libraries to the project classpath.
	 * 
	 * @param muleProject the mule project
	 * @throws JavaModelException
	 */
	protected void addMuleLibraries(IJavaProject muleProject) throws JavaModelException, MuleModelException {
		IClasspathEntry[] initial = muleProject.getRawClasspath();
		IClasspathEntry[] entries;
		if (projectPage.isChoosingLibsFromPlugin()) {
			entries = MuleClasspathUtils.getMulePluginLibraryEntries();
		} else {
			entries = MuleClasspathUtils.getExternalMuleLibraries(projectPage.getExternalRoot());
		}
		IClasspathEntry[] result = new IClasspathEntry[initial.length + entries.length];
		System.arraycopy(initial, 0, result, 0, initial.length);
		System.arraycopy(entries, 0, result, initial.length, entries.length);
		muleProject.setRawClasspath(result, new NullProgressMonitor());
	}

	/**
	 * Add the files from the sample project.
	 * 
	 * @param sample the sample
	 * @param project the Java project
	 */
	protected void addFromSample(Sample sample, IJavaProject project) {
		if (sample == null) {
			return;
		}

		// Find the bundle to copy from.
		Bundle bundle = Platform.getBundle(sample.getPluginId());
		if (bundle == null) {
			return;
		}

		// Locate the bundle entries based on extension attributes.
		String src = IPath.SEPARATOR + sample.getRoot() + IPath.SEPARATOR + sample.getSourcePath();
		String conf = IPath.SEPARATOR + sample.getRoot() + IPath.SEPARATOR + sample.getConfigPath();
		Enumeration srcs = bundle.findEntries(src, "*", true);
		Enumeration confs = bundle.findEntries(conf, "*", true);

		// Copy source files.
		try {
			IContainer sourceFolder = getSourceContainer(project);
			if (srcs != null) {
				while (srcs.hasMoreElements()) {
					URL url = (URL) srcs.nextElement();
					copyIntoProject(url, sourceFolder);
				}
			}
		} catch (JavaModelException e) {
			MuleCorePlugin.getDefault().logException("Unable to find a source folder.", e);
		}

		// Copy configuration files.
		try {
			IFolder configFolder = project.getProject().getFolder(CONFIG_FOLDER_NAME);
			configFolder.create(true, true, new NullProgressMonitor());
			Map configs = new HashMap();
			if (confs != null) {
				while (confs.hasMoreElements()) {
					URL url = (URL) confs.nextElement();
					IResource config = copyIntoProject(url, configFolder);
					if (config != null) {
						configs.put(config.getName(), config);
					}
				}
			}
			addConfigSets(sample, configs, project.getProject());
		} catch (CoreException e) {
			MuleCorePlugin.getDefault().logException("Unable to create config folder.", e);
		}
	}

	/**
	 * Get the first source folder from the project.
	 * 
	 * @param project the Java project
	 * @return the folder
	 * @throws JavaModelException
	 */
	protected IContainer getSourceContainer(IJavaProject project) throws JavaModelException {
		IPackageFragmentRoot[] roots = project.getPackageFragmentRoots();
		for (int i = 0; i < roots.length; i++) {
			if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE) {
				return (IContainer) roots[i].getResource();
			}
		}
		return null;
	}

	/**
	 * Copy a file or directory from a URL into a file on the project.
	 * 
	 * @param input
	 * @param project
	 */
	protected IResource copyIntoProject(URL input, IContainer parent) {
		try {
			URL local = Platform.asLocalURL(input);
			File inputFile = new File(local.getPath());
			IPath relative = new Path(input.getFile());
			relative = relative.removeFirstSegments(2);

			// Do not copy CVS entries.
			if (relative.toString().indexOf("CVS") != -1) {
				return null;
			}

			// Copy directories.
			if (inputFile.isDirectory()) {
				IFolder folder = parent.getFolder(relative);
				folder.create(true, true, new NullProgressMonitor());
				return folder;
			}

			// Copy files.
			else if (inputFile.isFile()) {
				IFile file = parent.getFile(relative);
				file.create(input.openStream(), true, new NullProgressMonitor());
				return file;
			}
		} catch (IOException e) {
			MuleCorePlugin.getDefault().logException("Unable to copy sample resource.", e);
		} catch (CoreException e) {
			MuleCorePlugin.getDefault().logException("Unable to create resource.", e);
		}
		return null;
	}

	/**
	 * Add config sets based on the list specified in the sample extension.
	 * 
	 * @param sample the sample
	 * @param resources the resources map
	 * @param project the project
	 * @throws MuleModelException
	 */
	protected void addConfigSets(Sample sample, Map resources, IProject project) throws MuleModelException {
		MuleNature nature = MuleCorePlugin.getDefault().getMuleNature(project);
		IMuleModel model = nature.getMuleModel().createWorkingCopy();
		ConfigSet[] configs = sample.getConfigSets();
		for (int i = 0; i < configs.length; i++) {
			ConfigSet config = configs[i];
			IFile file = (IFile) resources.get(config.getConfigPath());
			if (file != null) {
				IMuleConfiguration newConfig = model.createNewMuleConfiguration(config.getConfigName(), file
						.getProjectRelativePath().toString());
				model.addMuleConfiguration(newConfig);
				IMuleConfigSet newSet = model.createNewMuleConfigSet(config.getName());
				newSet.addConfiguration(newConfig);
				model.addMuleConfigSet(newSet);
			}
		}
		model.save();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench,
	 * org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.workbench = workbench;
	}

	/**
	 * Get the workbench that hosts the wizard.
	 * 
	 * @return the workbench
	 */
	protected IWorkbench getWorkbench() {
		return workbench;
	}

	/**
	 * Set the workbench that hosts the wizard.
	 * 
	 * @param workbench the workbench
	 */
	protected void setWorkbench(IWorkbench workbench) {
		this.workbench = workbench;
	}
}