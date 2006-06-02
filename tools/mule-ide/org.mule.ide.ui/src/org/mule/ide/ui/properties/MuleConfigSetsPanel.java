package org.mule.ide.ui.properties;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.mule.ide.core.model.IMuleConfigSet;
import org.mule.ide.core.model.IMuleConfiguration;
import org.mule.ide.core.model.IMuleModel;
import org.mule.ide.ui.IMuleImages;
import org.mule.ide.ui.MulePlugin;
import org.mule.ide.ui.MuleUIUtils;
import org.mule.ide.ui.dialogs.MuleConfigSetDialog;
import org.mule.ide.ui.model.MuleModelContentProvider;
import org.mule.ide.ui.model.MuleModelLabelProvider;
import org.mule.ide.ui.model.MuleModelViewerSorter;

/**
 * Panel that allows the config sets for the Mule IDE project to be changed.
 */
public class MuleConfigSetsPanel implements IMulePropertyPanel {

	/** The Mule model for the project */
	private IMuleModel muleModel;

	/** Holds the config sets list */
	private TableViewer configSetsTable;

	/** Holds the list of configs in a config set */
	private TableViewer configsTable;

	/** Button for adding a config set */
	private Button buttonSetAdd;

	/** Button for editing a config set */
	private Button buttonSetEdit;

	/** Button for deleting a config set */
	private Button buttonSetDelete;

	/** Button for adding a config file */
	private Button buttonConfigAdd;

	/** Button for moving a config file up */
	private Button buttonConfigUp;

	/** Button for moving a config file down */
	private Button buttonConfigDown;

	/** Button for deleting a config file */
	private Button buttonConfigDelete;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.ui.properties.IMulePropertyPanel#getDisplayName()
	 */
	public String getDisplayName() {
		return "Configuration Sets";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.ui.properties.IMulePropertyPanel#getImage()
	 */
	public Image getImage() {
		return MulePlugin.getDefault().getImage(IMuleImages.KEY_MULE_LOGO);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.ui.properties.IMulePropertyPanel#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public Composite createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout mainLayout = new GridLayout();
		mainLayout.numColumns = 2;
		composite.setLayout(mainLayout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		createConfigSetsArea(composite);
		createConfigFilesArea(composite);

		return composite;
	}

	/**
	 * Create the area that contains the config sets table and associated buttons.
	 * 
	 * @param composite the parent composite
	 */
	protected void createConfigSetsArea(Composite composite) {
		setConfigSetsTable(new TableViewer(composite, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL
				| SWT.BORDER));
		getConfigSetsTable().setLabelProvider(
				MuleModelLabelProvider.getDecoratingMuleModelLabelProvider());
		getConfigSetsTable().setContentProvider(new MuleModelContentProvider(false, true));
		getConfigSetsTable().setSorter(new MuleModelViewerSorter());
		getConfigSetsTable().getTable().setLayoutData(new GridData(GridData.FILL_BOTH));

		// Listen for selection changes in the config set table.
		getConfigSetsTable().addPostSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				configSetSelected(getSelectedConfigSet());
			}
		});

		// Double clicking a set acts like clicking the edit button.
		getConfigSetsTable().addDoubleClickListener(new IDoubleClickListener() {

			public void doubleClick(DoubleClickEvent event) {
				editSetClicked();
			}
		});

		// Create the config set buttons.
		Composite csButtons = MuleUIUtils.createButtonPanel(composite);
		buttonSetAdd = MuleUIUtils.createSideButton("Add", csButtons);
		buttonSetAdd.addMouseListener(new MouseAdapter() {
			public void mouseUp(MouseEvent e) {
				addSetClicked();
			}
		});
		buttonSetEdit = MuleUIUtils.createSideButton("Edit", csButtons);
		buttonSetEdit.addMouseListener(new MouseAdapter() {
			public void mouseUp(MouseEvent e) {
				editSetClicked();
			}
		});
		buttonSetDelete = MuleUIUtils.createSideButton("Delete", csButtons);
		buttonSetDelete.addMouseListener(new MouseAdapter() {
			public void mouseUp(MouseEvent e) {
				deleteSetClicked();
			}
		});
	}

	/**
	 * Return the config set selected in the config sets table.
	 * 
	 * @return the config set or null if none is selected
	 */
	protected IMuleConfigSet getSelectedConfigSet() {
		IStructuredSelection selection = (IStructuredSelection) getConfigSetsTable().getSelection();
		if (!selection.isEmpty()) {
			return (IMuleConfigSet) selection.getFirstElement();
		} else {
			return null;
		}
	}

	/**
	 * Create the area that contains the config files and associated buttons.
	 * 
	 * @param composite the parent composite
	 */
	protected void createConfigFilesArea(Composite composite) {
		setConfigsTable(new TableViewer(composite, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL
				| SWT.BORDER));
		getConfigsTable().setLabelProvider(
				MuleModelLabelProvider.getDecoratingMuleModelLabelProvider());
		getConfigsTable().setContentProvider(new MuleModelContentProvider(false, true));
		getConfigsTable().getTable().setLayoutData(new GridData(GridData.FILL_BOTH));

		// Listen for selection changes in the config set table.
		getConfigsTable().addPostSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				configFileSelected(getSelectedConfigFile());
			}
		});

		// Create the buttons for modifying the config files.
		Composite cButtons = MuleUIUtils.createButtonPanel(composite);
		buttonConfigAdd = MuleUIUtils.createSideButton("Add", cButtons);
		buttonConfigAdd.addMouseListener(new MouseAdapter() {
			public void mouseUp(MouseEvent e) {
				addClicked();
			}
		});
		buttonConfigUp = MuleUIUtils.createSideButton("Up", cButtons);
		buttonConfigUp.addMouseListener(new MouseAdapter() {
			public void mouseUp(MouseEvent e) {
				upClicked();
			}
		});
		buttonConfigDown = MuleUIUtils.createSideButton("Down", cButtons);
		buttonConfigDown.addMouseListener(new MouseAdapter() {
			public void mouseUp(MouseEvent e) {
				downClicked();
			}
		});
		buttonConfigDelete = MuleUIUtils.createSideButton("Delete", cButtons);
		buttonConfigDelete.addMouseListener(new MouseAdapter() {
			public void mouseUp(MouseEvent e) {
				deleteClicked();
			}
		});
	}

	/**
	 * Return the config set selected in the config sets table.
	 * 
	 * @return the config set or null if none is selected
	 */
	protected IMuleConfiguration getSelectedConfigFile() {
		IStructuredSelection selection = (IStructuredSelection) getConfigsTable().getSelection();
		if (!selection.isEmpty()) {
			return (IMuleConfiguration) selection.getFirstElement();
		} else {
			return null;
		}
	}

	/**
	 * Called when the config set selection changes.
	 * 
	 * @param configSet the config set or null if none selected
	 */
	protected void configSetSelected(IMuleConfigSet configSet) {
		// Show the contents of the config set in the configs table.
		getConfigsTable().setInput(configSet);

		// If available, set the selection to the first config in the set.
		if (configSet != null) {
			Iterator it = configSet.getMuleConfigurations().iterator();
			if (it.hasNext()) {
				IMuleConfiguration config = (IMuleConfiguration) it.next();
				getConfigsTable().setSelection(new StructuredSelection(config));
			}
		}
	}

	/**
	 * Called when a config file is selected.
	 * 
	 * @param configFile the file that was selected
	 */
	protected void configFileSelected(IMuleConfiguration configFile) {
		updateConfigFileButtonEnablement(configFile);
	}

	/**
	 * Update the enablement of the buttons based on the selection.
	 * 
	 * @param configFile the selected config file
	 */
	protected void updateConfigFileButtonEnablement(IMuleConfiguration configFile) {
		IMuleConfigSet configSet = getSelectedConfigSet();
		if ((configSet != null) && (configFile != null)) {
			if (configSet.isFirstConfiguration(configFile)) {
				buttonConfigUp.setEnabled(false);
			} else {
				buttonConfigUp.setEnabled(true);
			}
			if (configSet.isLastConfiguration(configFile)) {
				buttonConfigDown.setEnabled(false);
			} else {
				buttonConfigDown.setEnabled(true);
			}
			buttonConfigDelete.setEnabled(true);
		} else {
			buttonConfigUp.setEnabled(false);
			buttonConfigDown.setEnabled(false);
			buttonConfigDelete.setEnabled(false);
		}
	}

	/**
	 * Button for adding a config set was clicked.
	 */
	protected void addSetClicked() {
		MuleConfigSetDialog dialog = new MuleConfigSetDialog(getConfigSetsTable().getTable()
				.getShell());
		if (dialog.open() == Window.OK) {
			IMuleConfigSet configSet = getMuleModel().createNewMuleConfigSet(
					dialog.getDescription());
			getMuleModel().addMuleConfigSet(configSet);
			getConfigSetsTable().refresh();
			getConfigSetsTable().setSelection(new StructuredSelection(configSet));
		}
	}

	/**
	 * Button for adding a config set was clicked.
	 */
	protected void editSetClicked() {
		IMuleConfigSet configSet = getSelectedConfigSet();
		MuleConfigSetDialog dialog = new MuleConfigSetDialog(getConfigSetsTable().getTable()
				.getShell());
		dialog.setDescription(configSet.getDescription());
		if (dialog.open() == Window.OK) {
			configSet.setDescription(dialog.getDescription());
			getConfigSetsTable().refresh();
		}
	}

	/**
	 * Button for adding a config set was clicked.
	 */
	protected void deleteSetClicked() {
		IMuleConfigSet configSet = getSelectedConfigSet();
		getMuleModel().removeMuleConfigSet(configSet.getId());
		getConfigSetsTable().refresh();
	}

	/**
	 * Called when the button for adding configs is clicked.
	 */
	protected void addClicked() {
		IMuleConfigSet configSet = getSelectedConfigSet();
		List dupsRemoved = new ArrayList(configSet.getMuleModel().getMuleConfigurations());
		Iterator it = configSet.getMuleConfigurations().iterator();
		while (it.hasNext()) {
			IMuleConfiguration config = (IMuleConfiguration) it.next();
			dupsRemoved.remove(config);
		}
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(buttonConfigAdd
				.getShell(), MuleModelLabelProvider.getDecoratingMuleModelLabelProvider());
		dialog.setTitle("Add Mule Config Files");
		dialog.setMessage("Select the Mule configuration files to add");
		dialog.setMultipleSelection(true);
		dialog.setElements(dupsRemoved.toArray());
		if (dialog.open() == Window.OK) {
			Object[] results = dialog.getResult();
			for (int i = 0; i < results.length; i++) {
				IMuleConfiguration config = (IMuleConfiguration) results[i];
				configSet.addConfiguration(config);
			}
			getConfigsTable().refresh();
		}
	}

	/**
	 * Called when the button for moving a config up in priority is clicked.
	 */
	protected void upClicked() {
		IMuleConfigSet configSet = getSelectedConfigSet();
		IMuleConfiguration config = getSelectedConfigFile();
		if ((configSet != null) && (config != null)) {
			configSet.increasePriority(config);
			getConfigsTable().refresh();
			getConfigsTable().setSelection(new StructuredSelection(config));
		}
	}

	/**
	 * Called when the button for moving a config down in priority is clicked.
	 */
	protected void downClicked() {
		IMuleConfigSet configSet = getSelectedConfigSet();
		IMuleConfiguration config = getSelectedConfigFile();
		if ((configSet != null) && (config != null)) {
			configSet.decreasePriority(config);
			getConfigsTable().refresh();
			getConfigsTable().setSelection(new StructuredSelection(config));
		}
	}

	/**
	 * Called when the delete button is clicked.
	 */
	protected void deleteClicked() {
		IMuleConfigSet configSet = getSelectedConfigSet();
		IMuleConfiguration config = getSelectedConfigFile();
		if ((configSet != null) && (config != null)) {
			configSet.removeConfiguration(config);
			getConfigsTable().refresh();
			updateConfigFileButtonEnablement(null);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.ui.properties.IMulePropertyPanel#initialize(org.mule.ide.core.model.IMuleModel)
	 */
	public void initialize(IMuleModel model) {
		this.setMuleModel(model);

		// Set the model as the input for the config sets table.
		getConfigSetsTable().setInput(model);

		// If available, set the selection to the first config set.
		if (model != null) {
			Iterator it = model.getMuleConfigSets().iterator();
			if (it.hasNext()) {
				IMuleConfigSet configSet = (IMuleConfigSet) it.next();
				getConfigSetsTable().setSelection(new StructuredSelection(configSet));
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.ui.properties.IMulePropertyPanel#contentsChanged()
	 */
	public boolean contentsChanged() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.ui.properties.IMulePropertyPanel#commit(org.mule.ide.core.model.IMuleModel)
	 */
	public void commit(IMuleModel project) {
	}

	/**
	 * Sets the 'configSetsTable' field.
	 * 
	 * @param configSetsTable The 'configsTable' value.
	 */
	protected void setConfigSetsTable(TableViewer configSetsTable) {
		this.configSetsTable = configSetsTable;
	}

	/**
	 * Returns the 'configSetsTable' field.
	 * 
	 * @return the 'configSetsTable' field value.
	 */
	protected TableViewer getConfigSetsTable() {
		return configSetsTable;
	}

	/**
	 * Sets the 'configsTable' field.
	 * 
	 * @param configsTable The 'configsTable' value.
	 */
	protected void setConfigsTable(TableViewer configsTable) {
		this.configsTable = configsTable;
	}

	/**
	 * Returns the 'configsTable' field.
	 * 
	 * @return the 'configsTable' field value.
	 */
	protected TableViewer getConfigsTable() {
		return configsTable;
	}

	/**
	 * Sets the 'muleModel' field.
	 * 
	 * @param muleModel The 'muleModel' value.
	 */
	protected void setMuleModel(IMuleModel muleModel) {
		this.muleModel = muleModel;
	}

	/**
	 * Returns the 'muleModel' field.
	 * 
	 * @return the 'muleModel' field value.
	 */
	protected IMuleModel getMuleModel() {
		return muleModel;
	}
}