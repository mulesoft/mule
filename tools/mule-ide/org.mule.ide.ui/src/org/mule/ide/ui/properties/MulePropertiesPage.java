package org.mule.ide.ui.properties;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.dialogs.PropertyPage;
import org.mule.ide.core.MuleCorePlugin;
import org.mule.ide.core.exception.MuleModelException;
import org.mule.ide.core.model.IMuleModel;
import org.mule.ide.ui.MulePlugin;

public class MulePropertiesPage extends PropertyPage {

	/** The working copy of Mule model data */
	private IMuleModel workingCopy;

	/** The list of property panels for the project */
	private List propertyPanels = new ArrayList();

	/** Text shown by the nature checkbox */
	private static final String NATURE_MESSAGE = "&Mule UMO project";

	/** Nature checkbox widget */
	private Button hasNatureButton;

	/**
	 * Get the list of property panels.
	 * 
	 * @return the list a property panels
	 */
	protected List getPropertyPanels() {
		return propertyPanels;
	}

	/**
	 * Add a tab for the given panel. The panel will be initialized and commited automatically.
	 * 
	 * @param tabName the name that will be displayed on the tab for the panel
	 * @param panel the panel for the UI
	 */
	protected void addPropertyPanel(IMulePropertyPanel panel) {
		getPropertyPanels().add(panel);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		// No need for default or apply buttons.
		noDefaultAndApplyButton();

		// Try to load the Mule model. Show error if not found.
		IMuleModel model = getMuleModel();
		if (model == null) {
			return notMuleProjectWarning(parent);
		}

		// Create the working copy that will be used to init and commit all panels.
		try {
			setWorkingCopy(model.createWorkingCopy());
		} catch (MuleModelException e) {
			MuleCorePlugin.getDefault().getLog().log(e.getStatus());
			return notMuleProjectWarning(parent);
		}

		// Create the panels.
		createPropertyPanels(getWorkingCopy());

		// Create a tabbed folder that will hold the property panels.
		TabFolder tabs = new TabFolder(parent, SWT.NULL);
		for (Iterator it = getPropertyPanels().iterator(); it.hasNext();) {
			IMulePropertyPanel panel = (IMulePropertyPanel) it.next();
			TabItem tab = new TabItem(tabs, SWT.NULL);
			tab.setText(panel.getDisplayName());
			tab.setImage(panel.getImage());
			Composite control = panel.createControl(tabs);
			panel.initialize(getWorkingCopy());
			tab.setControl(control);
		}
		return tabs;
	}

	/**
	 * Create the property panels that will be shown in the UI.
	 * 
	 * @param gaijinProject the parent project
	 */
	protected void createPropertyPanels(IMuleModel muleModel) {
		addPropertyPanel(new MuleConfigurationsPanel());
		addPropertyPanel(new MuleConfigSetsPanel());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferencePage#performOk()
	 */
	public boolean performOk() {
		// If nature checkbox was shown, process it.
		if (hasNatureButton != null) {
			try {
				MuleCorePlugin.getDefault().setMuleNature(getProject(getElement()),
						hasNatureButton.getSelection());
			} catch (CoreException e) {
				return false;
			}
			return true;
		}

		// Otherwise, process all of the property panels.
		for (Iterator it = getPropertyPanels().iterator(); it.hasNext();) {
			IMulePropertyPanel panel = (IMulePropertyPanel) it.next();
			if (panel.contentsChanged()) {
				panel.commit(getWorkingCopy());
			}
		}
		try {
			getWorkingCopy().save();
		} catch (MuleModelException e) {
			MulePlugin.getDefault().showError("Unable to save Mule settings.", e.getStatus());
		}
		return true;
	}

	/**
	 * If no IGaijinProject was found, show an error label in the page.
	 * 
	 * @param parent
	 * @return
	 */
	protected Control notMuleProjectWarning(Composite parent) {
		setValid(true);
		return addNatureCheckboxSection(parent);
	}

	/**
	 * Create a section that allows the user to add the Mule nature to the project.
	 * 
	 * @param parent the parent composite
	 * @return the created composite
	 */
	private Composite addNatureCheckboxSection(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		hasNatureButton = new Button(composite, SWT.CHECK);
		hasNatureButton.setText(NATURE_MESSAGE);
		hasNatureButton.setSelection(MuleCorePlugin.getDefault().getMuleNature(
				getProject(getElement())) != null);
		hasNatureButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		return composite;
	}

	/**
	 * Get the project associated with the selected element.
	 * 
	 * @param element the selected element
	 * @return the project
	 */
	protected IProject getProject(IAdaptable element) {
		if (element instanceof IProject) {
			return (IProject) element;
		} else {
			return (IProject) (element.getAdapter(IProject.class));
		}
	}

	/**
	 * Get the Mule IDE model for the current project.
	 * 
	 * @return the model
	 */
	protected IMuleModel getMuleModel() {
		try {
			return MuleCorePlugin.getDefault().getMuleModel(getProject(getElement()));
		} catch (MuleModelException e) {
			return null;
		}
	}

	/**
	 * Set the working copy.
	 * 
	 * @param workingCopy the working copy
	 */
	protected void setWorkingCopy(IMuleModel workingCopy) {
		this.workingCopy = workingCopy;
	}

	/**
	 * Get the working copy
	 * 
	 * @return the working copy
	 */
	protected IMuleModel getWorkingCopy() {
		return workingCopy;
	}
}