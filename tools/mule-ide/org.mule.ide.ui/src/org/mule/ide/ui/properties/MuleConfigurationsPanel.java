package org.mule.ide.ui.properties;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
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
import org.mule.ide.core.model.IMuleConfiguration;
import org.mule.ide.core.model.IMuleModel;
import org.mule.ide.ui.IMuleImages;
import org.mule.ide.ui.MulePlugin;
import org.mule.ide.ui.MuleUIUtils;
import org.mule.ide.ui.dialogs.MuleConfigurationDialog;
import org.mule.ide.ui.model.MuleModelContentProvider;
import org.mule.ide.ui.model.MuleModelLabelProvider;
import org.mule.ide.ui.model.MuleModelViewerSorter;

/**
 * Panel that allows the configuration files for the Mule IDE project to be changed.
 */
public class MuleConfigurationsPanel implements IMulePropertyPanel {

	/** The parent model */
	private IMuleModel muleModel;

	/** Holds the configurations list */
	private TableViewer configsTable;

	/** Button for adding a new config */
	private Button addButton;

	/** Button for editing a config */
	private Button editButton;

	/** Button for deleting a config */
	private Button deleteButton;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.ui.properties.IMulePropertyPanel#getDisplayName()
	 */
	public String getDisplayName() {
		return "Configuration Files";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.ui.properties.IMulePropertyPanel#getImage()
	 */
	public Image getImage() {
		return MulePlugin.getDefault().getImage(IMuleImages.KEY_MULE_CONFIG);
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

		// Add the viewer that shows the config file list.
		setConfigsTable(new TableViewer(composite));
		getConfigsTable().setLabelProvider(
				MuleModelLabelProvider.getDecoratingMuleModelLabelProvider());
		getConfigsTable().setContentProvider(new MuleModelContentProvider(true, false));
		getConfigsTable().setSorter(new MuleModelViewerSorter());
		getConfigsTable().getTable().setLayoutData(new GridData(GridData.FILL_BOTH));

		// Double clicking a config acts like clicking the edit button.
		getConfigsTable().addDoubleClickListener(new IDoubleClickListener() {

			public void doubleClick(DoubleClickEvent event) {
				editClicked();
			}
		});

		// Create buttons and add button handlers.
		Composite buttons = MuleUIUtils.createButtonPanel(composite);
		addButton = MuleUIUtils.createSideButton("Add", buttons);
		addButton.addMouseListener(new MouseAdapter() {
			public void mouseUp(MouseEvent e) {
				addClicked();
			}
		});
		editButton = MuleUIUtils.createSideButton("Edit", buttons);
		editButton.addMouseListener(new MouseAdapter() {
			public void mouseUp(MouseEvent e) {
				editClicked();
			}
		});
		deleteButton = MuleUIUtils.createSideButton("Delete", buttons);
		deleteButton.addMouseListener(new MouseAdapter() {
			public void mouseUp(MouseEvent e) {
				deleteClicked();
			}
		});

		return composite;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.ui.properties.IMulePropertyPanel#initialize(org.mule.ide.core.model.IMuleModel)
	 */
	public void initialize(IMuleModel model) {
		this.muleModel = model;
		getConfigsTable().setInput(model);
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
	 * Add button was clicked.
	 */
	protected void addClicked() {
		MuleConfigurationDialog dialog = new MuleConfigurationDialog(muleModel.getProject(),
				getConfigsTable().getTable().getShell());
		if (dialog.open() == Window.OK) {
			IMuleConfiguration config = muleModel.createNewMuleConfiguration(
					dialog.getDescription(), dialog.getPath());
			muleModel.addMuleConfiguration(config);
			getConfigsTable().refresh();
		}
	}

	/**
	 * Edit button was clicked.
	 */
	protected void editClicked() {
		// Get the current selection.
		IStructuredSelection selection = (IStructuredSelection) getConfigsTable().getSelection();
		IMuleConfiguration config = (IMuleConfiguration) selection.getFirstElement();
		if (config == null) {
			return;
		}

		// Load the selection in the dialog.
		MuleConfigurationDialog dialog = new MuleConfigurationDialog(muleModel.getProject(),
				getConfigsTable().getTable().getShell());
		dialog.setDescription(config.getDescription());
		dialog.setPath(config.getRelativePath());
		if (dialog.open() == Window.OK) {
			muleModel.removeMuleConfiguration(config.getId());
			IMuleConfiguration newConfig = muleModel.createNewMuleConfiguration(
					dialog.getDescription(), dialog.getPath());
			muleModel.addMuleConfiguration(newConfig);
			getConfigsTable().refresh();
		}
	}

	/**
	 * Delete button was clicked.
	 */
	protected void deleteClicked() {
		IStructuredSelection selection = (IStructuredSelection) getConfigsTable().getSelection();
		IMuleConfiguration config = (IMuleConfiguration) selection.getFirstElement();
		if (config != null) {
			muleModel.removeMuleConfiguration(config.getId());
			getConfigsTable().refresh();
		}
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
}