package org.mule.ide.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPage;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.mule.ide.core.MuleCorePlugin;
import org.mule.ide.ui.MulePlugin;

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.IWizard#addPages()
	 */
	public void addPages() {
		projectPage = new MuleWizardProjectPage();
		addPage(projectPage);
		javaPage = new NewJavaProjectWizardPage(null, projectPage);
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
			addMuleClasspathContainer(javaProject);
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
	 * Add the Mule classpath container to the project classpath.
	 * 
	 * @param muleProject the mule project
	 * @throws JavaModelException
	 */
	protected void addMuleClasspathContainer(IJavaProject muleProject) throws JavaModelException {
		IClasspathEntry[] entries = muleProject.getRawClasspath();
		IClasspathEntry muleContainer = MuleCorePlugin.createMuleClasspathContainerEntry();
		List newEntries = new ArrayList();
		boolean addedMuleContainer = false;
		for (int i = 0; i < entries.length; i++) {
			newEntries.add(entries[i]);
			if (entries[i].getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
				newEntries.add(muleContainer);
				addedMuleContainer = true;
			}
		}
		if (!addedMuleContainer) {
			newEntries.add(muleContainer);
		}
		muleProject.setRawClasspath((IClasspathEntry[]) newEntries
				.toArray(new IClasspathEntry[newEntries.size()]), new NullProgressMonitor());
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