package org.mule.ide.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 * Commonly used UI functionality.
 */
public class MuleUIUtils {

	/**
	 * Creates a button panel on the right side of a parent composite.
	 * 
	 * @param parent the parent composite
	 * @return the created panel
	 */
	public static Composite createButtonPanel(Composite parent) {
		Composite buttons = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 10;
		layout.marginWidth = 10;
		layout.marginHeight = 10;
		buttons.setLayout(new GridLayout());
		GridData data = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_END);
		buttons.setLayoutData(data);
		return buttons;
	}

	/**
	 * Creates a fixed-width button with a label on a parent composite.
	 * 
	 * @param label the label
	 * @param parent the parent composite
	 * @return the button
	 */
	public static Button createSideButton(String label, Composite parent) {
		Button button = new Button(parent, SWT.NULL);
		button.setText(label);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 100;
		button.setLayoutData(data);
		return button;
	}

}