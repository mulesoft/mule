/*
 * $Header$
 * $Revision$
 * $Date$
 */
package org.mule.ide.ui.model;

import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IViewerLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ViewerLabel;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.mule.ide.core.model.IMuleConfigSet;
import org.mule.ide.core.model.IMuleConfiguration;
import org.mule.ide.core.model.IMuleModelElement;
import org.mule.ide.ui.IMuleImages;
import org.mule.ide.ui.MulePlugin;

/**
 * Handles labels and images associated with elements in the Mule model.
 */
public class MuleModelLabelProvider extends LabelProvider implements IViewerLabelProvider {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		if (element instanceof IMuleConfigSet) {
			return MulePlugin.getDefault().getImage(IMuleImages.KEY_MULE_LOGO);
		} else if (element instanceof IMuleConfiguration) {
			return MulePlugin.getDefault().getImage(IMuleImages.KEY_MULE_CONFIG);
		}
		return super.getImage(element);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element) {
		if (element instanceof IMuleModelElement) {
			return ((IMuleModelElement) element).getLabel();
		}
		return super.getText(element);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IViewerLabelProvider#updateLabel(org.eclipse.jface.viewers.ViewerLabel,
	 * java.lang.Object)
	 */
	public void updateLabel(ViewerLabel label, Object element) {
	}

	/**
	 * Get a Mule model label provider that indicates marker status on underlying resources.
	 * 
	 * @return the decorating label provider
	 */
	public static ILabelProvider getDecoratingMuleModelLabelProvider() {
		return new DecoratingLabelProvider(new MuleModelLabelProvider(), PlatformUI.getWorkbench()
				.getDecoratorManager().getLabelDecorator());
	}
}