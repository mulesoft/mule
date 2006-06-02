package org.mule.ide.ui.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.mule.ide.core.IMuleDefaults;
import org.mule.ide.core.MuleCorePlugin;

/**
 * Dialog for editing a mule configuration entry.
 */
public class MuleConfigurationDialog extends Dialog implements ModifyListener {

	/** Parent project */
	private IProject project;

	/** Widget for description */
	private Text textDescription;

	/** Widget for file path */
	private Text textPath;

	/** Button for browsing to a file path */
	private Button buttonBrowse;

	/** Description value */
	private String description = "";

	/** File path value */
	private String path = "";

	/**
	 * Create the dialog.
	 * 
	 * @param parent the parent shell
	 */
	public MuleConfigurationDialog(IProject project, Shell parent) {
		super(parent);
		this.project = project;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		GridData gd;
		getShell().setText("Mule Configuration File");
		Composite composite = (Composite) super.createDialogArea(parent);
		GridLayout layout = (GridLayout) composite.getLayout();
		layout.numColumns = 3;
		Label descLabel = new Label(composite, SWT.NULL);
		descLabel.setText("Description");
		textDescription = new Text(composite, SWT.BORDER);
		textDescription.setText(getDescription());
		textDescription.addModifyListener(this);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 200;
		gd.horizontalSpan = 2;
		textDescription.setLayoutData(gd);
		Label pathLabel = new Label(composite, SWT.NULL);
		pathLabel.setText("File Path");
		textPath = new Text(composite, SWT.BORDER);
		textPath.setText(getPath());
		textPath.addModifyListener(this);
		textPath.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		buttonBrowse = new Button(composite, SWT.NULL);
		buttonBrowse.setText("...");
		buttonBrowse.addMouseListener(new MouseAdapter() {
			public void mouseUp(MouseEvent e) {
				browse();
			}
		});
		return composite;
	}

	/**
	 * Called when text in the fields is modified.
	 * 
	 * @param e
	 */
	public void modifyText(ModifyEvent e) {
		if (e.getSource() == textDescription) {
			description = textDescription.getText();
		} else if (e.getSource() == textPath) {
			path = textPath.getText();
		}
	}

	/**
	 * Create a list of resources in the container that end with the given suffix.
	 * 
	 * @param inside the container
	 * @param suffix the suffix
	 * @param result the list of matches
	 * @throws CoreException
	 */
	protected void collect(IContainer inside, String suffix, List result) throws CoreException {
		if (inside == null) {
			return;
		}

		IResource[] resources = inside.members();
		for (int i = 0; i < resources.length; ++i) {
			if (resources[i] instanceof IContainer) {
				collect((IContainer) resources[i], suffix, result);
			} else if (resources[i] instanceof IFile) {
				if (resources[i].getName().endsWith(suffix))
					result.add(resources[i]);
			}
		}
	}

	/**
	 * Browse for a config file.
	 */
	protected void browse() {
		IFile chosen = chooseConfigFile();
		if (chosen != null) {
			setPath(chosen.getProjectRelativePath().toString());
		}
	}

	/**
	 * Browse for the config file path.
	 */
	protected IFile chooseConfigFile() {
		Shell shell = getShell();

		ILabelProvider labelProvider = new WorkbenchLabelProvider() {
			protected String decorateText(String input, Object element) {
				if (element instanceof IFile)
					return input + " - " + ((IFile) element).getProjectRelativePath().toString();
				return input;
			}
		};
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(),
				labelProvider);

		dialog.setTitle("Choose Mule Config File");
		dialog.setMessage("Select the Mule configuration file to add");
		try {
			List l = new ArrayList();
			collect(project, IMuleDefaults.MULE_CONFIG_FILE_SUFFIX, l);
			if (l.size() < 1) {
				MessageDialog.openError(shell, "Mule Config File",
						"No Mule config files in selected project");
				return null;
			}

			dialog.setElements(l.toArray());
			if (dialog.open() == Window.OK) {
				return (IFile) dialog.getFirstResult();
			}
		} catch (CoreException e) {
			MuleCorePlugin.getDefault().logException(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Set the description value.
	 * 
	 * @param description
	 */
	public void setDescription(String description) {
		this.description = description;
		if ((textDescription != null) && (!textDescription.isDisposed())) {
			textDescription.setText(description);
		}
	}

	/**
	 * Get the description value.
	 * 
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Set the path value.
	 * 
	 * @param path
	 */
	public void setPath(String path) {
		this.path = path;
		if ((textPath != null) && (!textPath.isDisposed())) {
			textPath.setText(path);
		}
	}

	/**
	 * Get the path value.
	 * 
	 * @return
	 */
	public String getPath() {
		return path;
	}
}