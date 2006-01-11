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

package org.mule.ide.launching;

public interface IMuleLaunchConfigurationConstants {

	/** Identifier for the Local Mule Server launch configuration type */
	public static final String ID_MULE_SERVER = MuleLaunchPlugin.PLUGIN_ID + ".localMuleServer";

	/** Attribute that holds the selected Mule project */
	String ATTR_MULE_EXEC_CLASS = "org.mule.MuleExecClass";

	/** Attribute that holds the selected Mule config set id */
	String ATTR_MULE_CONFIG_SET_ID = "org.mule.MuleConfigSetId";

	/** Default class to execute for a Mule launch */
	String DEFAULT_MULE_EXEC_CLASS = "org.mule.ide.core.server.MuleServerController";
}