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

import org.eclipse.ui.plugin.*;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.mule.ide.core.MuleCorePlugin;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class MulePlugin extends AbstractUIPlugin {

	static public final String PLUGIN_ID = "org.mule.ide.ui";
	
	//The shared instance.
	private static MulePlugin plugin;
	
    /**
     * Returns the preference store for this UI plug-in.
     * This preference store is used to hold persistent settings for this plug-in in
     * the context of a workbench. Some of these settings will be user controlled, 
     * whereas others may be internal setting that are never exposed to the user.
     * <p>
     * If an error occurs reading the preference store, an empty preference store is
     * quietly created, initialized with defaults, and returned.
     * </p>
     * <p>
     * <strong>NOTE:</strong> As of Eclipse 3.1 this method is
     * no longer referring to the core runtime compatibility layer and so
     * plug-ins relying on Plugin#initializeDefaultPreferences
     * will have to access the compatibility layer themselves.
     * </p>
     *
     * @return the preference store 
     */
	public IPreferenceStore myPreferenceStore;
	
    public IPreferenceStore getPreferenceStore() {
        // Create the preference store lazily.
        if (myPreferenceStore == null) {
        	myPreferenceStore = new ScopedPreferenceStore(new InstanceScope(),MuleCorePlugin.getDefault().getBundle().getSymbolicName());
        }
        return myPreferenceStore;
    }
	/**
	 * The constructor.
	 */
	public MulePlugin() {
		plugin = this;
	}

	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}

	/**
	 * This method is called when the plug-in is stopped
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
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path.
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return AbstractUIPlugin.imageDescriptorFromPlugin("org.mule.ide.ui", path);
	}
}
