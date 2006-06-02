package org.mule.ide.ui.panels;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.mule.ide.core.samples.SampleLoader;

/**
 * Allows the choice of initial project content.
 * 
 * @author dadams
 */
public class ProjectContentChooser {

	/** Button for loading an empty project */
	private Button buttonEmpty;

	/** Button for loading from a sample project */
	private Button buttonSample;

	/** Dropdown that contains a list of sample projects */
	private Combo samples;

	/** One of the LOAD_FROM_* constants */
	private int choice;

	/** Constant that indicates to load from an empty project */
	public static final int LOAD_FROM_EMPTY = 0;

	/** Constant that indicates to load from a sample project */
	public static final int LOAD_FROM_SAMPLE = 1;

	/**
	 * Create the widgets on a parent composite.
	 * 
	 * @param parent the parent composite
	 * @return the created composite
	 */
	public Composite createControl(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText("Choose the initial content of the project");
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		buttonEmpty = new Button(group, SWT.RADIO);
		buttonEmpty.setText("Empty project");
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		buttonEmpty.setLayoutData(data);
		buttonEmpty.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				setChoice(LOAD_FROM_EMPTY);
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				setChoice(LOAD_FROM_EMPTY);
			}
		});

		buttonSample = new Button(group, SWT.RADIO);
		buttonSample.setText("Sample project");
		buttonSample.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
		buttonSample.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				setChoice(LOAD_FROM_SAMPLE);
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				setChoice(LOAD_FROM_SAMPLE);
			}
		});

		samples = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
		samples.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		samples.setItems(SampleLoader.getInstance().getSampleDescriptions());

		return group;
	}

	/**
	 * Set the "load from" choice.
	 * 
	 * @param choice the choice of LOAD_FROM_* constant
	 */
	public void setChoice(int choice) {
		this.choice = choice;
		if (choice == LOAD_FROM_EMPTY) {
			if (!buttonEmpty.isDisposed()) {
				buttonEmpty.setSelection(true);
				buttonSample.setSelection(false);
				samples.setEnabled(false);
			}
		} else {
			if (!buttonSample.isDisposed()) {
				buttonEmpty.setSelection(false);
				buttonSample.setSelection(true);
				samples.setEnabled(true);
			}
		}
	}

	/**
	 * Ge the "load from" choice.
	 * 
	 * @return the choice of LOAD_FROM_* constant
	 */
	public int getChoice() {
		return choice;
	}

	/**
	 * Get the sample description that is chosen.
	 * 
	 * @return the description or null if empty project or no sample chosen
	 */
	public String getChosenSampleDescription() {
		if (choice == LOAD_FROM_EMPTY) {
			return null;
		}
		if (samples.getSelectionIndex() > -1) {
			return samples.getItem(samples.getSelectionIndex());
		} else {
			return null;
		}
	}
}