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

package org.mule.ide.ui.preferences;

/**
 * Constant definitions for plug-in preferences
 */
public class IPreferenceConstants {

	/** Indicates whether classpath is from plugin or external */
	public static final String MULE_CLASSPATH_TYPE = "muleClasspathType";

	/** Value constant for MULE_CLASSPATH_TYPE. Use core plugin for classpath. */
	public static final String MULE_CLASSPATH_TYPE_PLUGIN = "plugin";

	/** Value constant for MULE_CLASSPATH_TYPE. Use external classpath. */
	public static final String MULE_CLASSPATH_TYPE_EXTERNAL = "external";

	/** Location of external Mule installation root */
	public static final String EXTERNAL_MULE_ROOT = "externalMuleRoot";
}