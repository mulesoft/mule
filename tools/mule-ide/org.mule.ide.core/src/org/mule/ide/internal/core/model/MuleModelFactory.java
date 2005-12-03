/*
 * $Header$
 * $Revision$
 * $Date$
 */
package org.mule.ide.internal.core.model;

import org.mule.ide.ConfigFileType;
import org.mule.ide.MuleIDEFactory;
import org.mule.ide.core.model.IMuleConfiguration;
import org.mule.ide.core.model.IMuleModel;

/**
 * Converts between mule model components and EMF components.
 */
public class MuleModelFactory {

	/**
	 * Convert an EMF config file element to a Eclipse model element.
	 * 
	 * @param parent the parent model
	 * @param emfConfig the EMF element
	 * @return the model element
	 */
	public static IMuleConfiguration convert(IMuleModel parent, ConfigFileType emfConfig) {
		MuleConfiguration modelConfig = new MuleConfiguration(parent, emfConfig.getId(),
				emfConfig.getDescription(), emfConfig.getPath());
		return modelConfig;
	}

	/**
	 * Convert the Eclipse config file element to an EMF element.
	 * 
	 * @param modelConfig the Eclipse config file element
	 * @return the EMF config file element
	 */
	public static ConfigFileType convert(IMuleConfiguration modelConfig) {
		ConfigFileType config = MuleIDEFactory.eINSTANCE.createConfigFileType();
		config.setId(modelConfig.getId());
		config.setDescription(modelConfig.getDescription());
		config.setPath(modelConfig.getRelativePath());
		return config;
	}
}