package org.mule.ide.ui.properties;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.mule.ide.core.model.IMuleModel;

/**
 * Interface for panels that can commit changes to the Mule IDE model.
 */
public interface IMulePropertyPanel {

	/**
	 * Gets the name that will be displayed when referring to the panel.
	 * 
	 * @return the name
	 */
	public String getDisplayName();

	/**
	 * Get the image that will be displayed for the panel.
	 * 
	 * @return the image
	 */
	public Image getImage();

	/**
	 * Creates the panel UI on the parent composite.
	 * 
	 * @param parent
	 * @return
	 */
	public Composite createControl(Composite parent);

	/**
	 * Initialize the contents of the panel from a working copy of the model.
	 * 
	 * @param project
	 */
	public void initialize(IMuleModel model);

	/**
	 * Indicates whether the contents of the panel were changed.
	 * 
	 * @return
	 */
	public boolean contentsChanged();

	/**
	 * Commits changes from the panel into the working copy of the model.
	 * 
	 * @param project
	 */
	public void commit(IMuleModel project);
}