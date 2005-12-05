package org.mule.ide.ui.properties;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.mule.ide.core.model.IMuleModel;
import org.mule.ide.ui.IMuleImages;
import org.mule.ide.ui.MulePlugin;
import org.mule.ide.ui.MuleUIUtils;
import org.mule.ide.ui.model.MuleModelContentProvider;
import org.mule.ide.ui.model.MuleModelLabelProvider;
import org.mule.ide.ui.model.MuleModelViewerSorter;

/**
 * Panel that allows the config sets for the Mule IDE project to be changed.
 */
public class MuleConfigSetsPanel implements IMulePropertyPanel {

	/** Holds the config sets list */
	private TableViewer configSetsTable;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.ui.properties.IMulePropertyPanel#getDisplayName()
	 */
	public String getDisplayName() {
		return "Config Sets";
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

		setConfigSetsTable(new TableViewer(composite));
		getConfigSetsTable().setLabelProvider(
				MuleModelLabelProvider.getDecoratingMuleModelLabelProvider());
		getConfigSetsTable().setContentProvider(new MuleModelContentProvider(false, true));
		getConfigSetsTable().setSorter(new MuleModelViewerSorter());
		getConfigSetsTable().getTable().setLayoutData(new GridData(GridData.FILL_BOTH));

		Composite buttons = MuleUIUtils.createButtonPanel(composite);
		MuleUIUtils.createSideButton("Add", buttons);
		MuleUIUtils.createSideButton("Delete", buttons);

		return composite;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.ui.properties.IMulePropertyPanel#initialize(org.mule.ide.core.model.IMuleModel)
	 */
	public void initialize(IMuleModel model) {
		getConfigSetsTable().setInput(model);
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
}