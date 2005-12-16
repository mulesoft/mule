/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Jesper Steen Møller. All rights reserved.
 * http://www.selskabet.org/jesper/
 * 
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.ide.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.mule.ide.core.MuleCorePlugin;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class MulePlugin extends AbstractUIPlugin {

	static public final String PLUGIN_ID = "org.mule.ide.ui";

	// The shared instance.
	private static MulePlugin plugin;

	/**
	 * Returns the preference store for this UI plug-in. This preference store is used to hold
	 * persistent settings for this plug-in in the context of a workbench. Some of these settings
	 * will be user controlled, whereas others may be internal setting that are never exposed to the
	 * user.
	 * <p>
	 * If an error occurs reading the preference store, an empty preference store is quietly
	 * created, initialized with defaults, and returned.
	 * </p>
	 * <p>
	 * <strong>NOTE:</strong> As of Eclipse 3.1 this method is no longer referring to the core
	 * runtime compatibility layer and so plug-ins relying on Plugin#initializeDefaultPreferences
	 * will have to access the compatibility layer themselves.
	 * </p>
	 * 
	 * @return the preference store
	 */
	public IPreferenceStore myPreferenceStore;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#getPreferenceStore()
	 */
	public IPreferenceStore getPreferenceStore() {
		if (myPreferenceStore == null) {
			myPreferenceStore = new ScopedPreferenceStore(new InstanceScope(),
					MuleCorePlugin.getDefault().getBundle().getSymbolicName());
		}
		return myPreferenceStore;
	}

	/**
	 * The constructor.
	 */
	public MulePlugin() {
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		plugin = null;
	}

	/**
	 * Returns the shared instance.
	 */
	public static MulePlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in relative path.
	 * 
	 * @param path the path
	 * @return the image descriptor
	 */
	private ImageDescriptor getImageDescriptor(String path) {
		return AbstractUIPlugin.imageDescriptorFromPlugin("org.mule.ide.ui", path);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#initializeImageRegistry(org.eclipse.jface.resource.ImageRegistry)
	 */
	protected void initializeImageRegistry(ImageRegistry reg) {
		super.initializeImageRegistry(reg);
		reg.put(IMuleImages.KEY_MULE_LOGO, getImageDescriptor(IMuleImages.PATH_MULE_LOGO));
		reg.put(IMuleImages.KEY_MULE_CONFIG, getImageDescriptor(IMuleImages.PATH_MULE_CONFIG));
	}

	/**
	 * Get the image registered under the given key.
	 * 
	 * @param key the key
	 * @return the image
	 */
	public Image getImage(String key) {
		return getImageRegistry().get(key);
	}

	/**
	 * Show an error dialog with the given message.
	 * 
	 * @param message the message
	 */
	public void showError(String message, IStatus status) {
		if (getWorkbench().getActiveWorkbenchWindow() != null) {
			ErrorDialog.openError(getWorkbench().getActiveWorkbenchWindow().getShell(), "Error",
					message, status);
		}
	}
}