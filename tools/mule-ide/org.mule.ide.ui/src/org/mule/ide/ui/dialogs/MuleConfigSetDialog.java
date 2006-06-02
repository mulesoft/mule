package org.mule.ide.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog for editing a mule config set.
 */
public class MuleConfigSetDialog extends Dialog implements ModifyListener {

	/** Widget for description */
	private Text textDescription;

	/** Description value */
	private String description = "";

	/**
	 * Create the dialog.
	 * 
	 * @param parent the parent shell
	 */
	public MuleConfigSetDialog(Shell parent) {
		super(parent);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		GridData gd;
		getShell().setText("Mule Configuration Set");
		Composite composite = (Composite) super.createDialogArea(parent);
		GridLayout layout = (GridLayout) composite.getLayout();
		layout.numColumns = 2;
		Label descLabel = new Label(composite, SWT.NULL);
		descLabel.setText("Description");
		textDescription = new Text(composite, SWT.BORDER);
		textDescription.setText(getDescription());
		textDescription.addModifyListener(this);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 200;
		textDescription.setLayoutData(gd);
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
		}
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
}