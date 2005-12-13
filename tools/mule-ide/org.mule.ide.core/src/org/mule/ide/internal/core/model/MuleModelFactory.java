/*
 * $Header$
 * $Revision$
 * $Date$
 */
package org.mule.ide.internal.core.model;

import java.util.Iterator;

import org.eclipse.emf.common.util.EList;
import org.mule.ide.ConfigFileRefType;
import org.mule.ide.ConfigFileType;
import org.mule.ide.ConfigSetType;
import org.mule.ide.MuleIDEFactory;
import org.mule.ide.core.model.IMuleConfigSet;
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
	 * Convert an EMF config set element to an Eclipse model element. Resolves any config file id
	 * references against the model.
	 * 
	 * @param parent the parent model
	 * @param emfConfigSet the EMF config set element
	 * @return the model config set that was created
	 */
	public static IMuleConfigSet convert(IMuleModel parent, ConfigSetType emfConfigSet) {
		MuleConfigSet modelConfigSet = new MuleConfigSet(parent, emfConfigSet.getId(),
				emfConfigSet.getDescription());
		EList refs = emfConfigSet.getConfigFileRef();
		Iterator it = refs.iterator();
		while (it.hasNext()) {
			ConfigFileRefType ref = (ConfigFileRefType) it.next();
			IMuleConfiguration resolved = parent.getMuleConfiguration(ref.getId());
			if (resolved != null) {
				modelConfigSet.getMuleConfigurations().add(resolved);
			}
		}
		return modelConfigSet;
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

	/**
	 * Convert the Eclipse config set element to an EMF element.
	 * 
	 * @param modelConfigSet the Eclipse config set element
	 * @return the EMF config set element
	 */
	public static ConfigSetType convert(IMuleConfigSet modelConfigSet) {
		ConfigSetType configSet = MuleIDEFactory.eINSTANCE.createConfigSetType();
		configSet.setId(modelConfigSet.getId());
		configSet.setDescription(modelConfigSet.getDescription());
		Iterator it = modelConfigSet.getMuleConfigurations().iterator();
		while (it.hasNext()) {
			IMuleConfiguration referenced = (IMuleConfiguration) it.next();
			ConfigFileRefType refType = MuleIDEFactory.eINSTANCE.createConfigFileRefType();
			refType.setId(referenced.getId());
			configSet.getConfigFileRef().add(refType);
		}
		return configSet;
	}
}