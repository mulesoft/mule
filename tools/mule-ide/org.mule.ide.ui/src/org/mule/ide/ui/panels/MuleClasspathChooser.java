package org.mule.ide.ui.panels;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.mule.ide.ui.preferences.IPreferenceConstants;
import org.mule.ide.ui.preferences.MulePreferences;

/**
 * Widgets needed to choose the location from which Mule libraries are loaded.
 * 
 * @author Derek Adams
 */
public class MuleClasspathChooser {

	/** Button for loading libs from plugin classpath */
	private Button buttonPlugin;

	/** Button for loading libs from an external location */
	private Button buttonExternal;

	/** Text field for external root value */
	private Text textExternalRoot;

	/** Button for browsing for an external root */
	private Button buttonBrowse;

	/** Choice for location of libraries */
	private int libLocationChoice;

	/** Value for external root choice */
	private String externalRoot;

	/** Constant that indicates to load libs from the core plugin */
	public static final int LOAD_FROM_PLUGIN = 0;

	/** Constant that indicates to load libs from an external path */
	public static final int LOAD_FROM_EXTERNAL = 1;

	/**
	 * Initialize the values from the preferences store.
	 */
	public void initializeFromPreferences() {
		setExternalRoot(MulePreferences.getDefaultExternalMuleRoot());
		String cpType = MulePreferences.getDefaultClasspathChoice();
		if (IPreferenceConstants.MULE_CLASSPATH_TYPE_EXTERNAL.equals(cpType)) {
			setLibLocationChoice(LOAD_FROM_EXTERNAL);
		} else {
			setLibLocationChoice(LOAD_FROM_PLUGIN);
		}
	}

	/**
	 * Save the values into the preference store.
	 */
	public void saveToPreferences() {
		if (getLibLocationChoice() == LOAD_FROM_EXTERNAL) {
			MulePreferences.setDefaultClasspathChoice(IPreferenceConstants.MULE_CLASSPATH_TYPE_EXTERNAL);
		} else {
			MulePreferences.setDefaultClasspathChoice(IPreferenceConstants.MULE_CLASSPATH_TYPE_PLUGIN);
		}
		MulePreferences.setDefaultExternalMuleRoot(getExternalRoot());
	}

	/**
	 * Create the widgets on a parent composite.
	 * 
	 * @param parent the parent composite
	 * @return the created composite
	 */
	public Composite createControl(Composite parent) {
		Group cpGroup = new Group(parent, SWT.NONE);
		cpGroup.setText("Choose where Eclipse will look for Mule libraries");
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		cpGroup.setLayout(layout);
		cpGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		buttonPlugin = new Button(cpGroup, SWT.RADIO);
		buttonPlugin.setText("Mule plugin (jars included with Mini-Mule distribution)");
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 3;
		buttonPlugin.setLayoutData(data);
		buttonPlugin.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				setLibLocationChoice(LOAD_FROM_PLUGIN);
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				setLibLocationChoice(LOAD_FROM_PLUGIN);
			}
		});

		buttonExternal = new Button(cpGroup, SWT.RADIO);
		buttonExternal.setText("External installation");
		buttonExternal.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
		buttonExternal.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				setLibLocationChoice(LOAD_FROM_EXTERNAL);
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				setLibLocationChoice(LOAD_FROM_EXTERNAL);
			}
		});

		textExternalRoot = new Text(cpGroup, SWT.BORDER);
		textExternalRoot.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		buttonBrowse = new Button(cpGroup, SWT.PUSH);
		buttonBrowse.setText("...");
		buttonBrowse.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		buttonBrowse.addMouseListener(new MouseAdapter() {

			public void mouseDown(MouseEvent e) {
				browse();
			}
		});

		initializeFromPreferences();
		return cpGroup;
	}

	/**
	 * Browse for the external root.
	 */
	protected void browse() {
		DirectoryDialog dialog = new DirectoryDialog(buttonBrowse.getShell());
		String filepath = dialog.open();
		if (filepath != null) {
			setExternalRoot(filepath);
		}
	}

	/**
	 * @return Returns the externalRoot.
	 */
	public String getExternalRoot() {
		return externalRoot;
	}

	/**
	 * @param externalRoot The externalRoot to set.
	 */
	public void setExternalRoot(String externalRoot) {
		if (externalRoot == null) {
			externalRoot = "";
		}
		this.externalRoot = externalRoot;
		if (!textExternalRoot.isDisposed()) {
			textExternalRoot.setText(externalRoot);
		}
	}

	/**
	 * @return Returns the libLocationChoice.
	 */
	public int getLibLocationChoice() {
		return libLocationChoice;
	}

	/**
	 * @param libLocationChoice The libLocationChoice to set.
	 */
	public void setLibLocationChoice(int libLocationChoice) {
		this.libLocationChoice = libLocationChoice;
		if (libLocationChoice == LOAD_FROM_PLUGIN) {
			if (!buttonPlugin.isDisposed()) {
				buttonPlugin.setSelection(true);
				buttonExternal.setSelection(false);
				textExternalRoot.setEnabled(false);
				buttonBrowse.setEnabled(false);
			}
		} else {
			if (!buttonExternal.isDisposed()) {
				buttonPlugin.setSelection(false);
				buttonExternal.setSelection(true);
				textExternalRoot.setEnabled(true);
				buttonBrowse.setEnabled(true);
			}
		}
	}
}