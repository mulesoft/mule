package org.mule.ide.core.samples;

import org.mule.ide.core.model.IMuleConfigSet;

/**
 * Holds details about a sample project that can be loaded at Mule project creation.
 * 
 * @author Derek Adams
 */
public class Sample {

	/** Unique id for contributing plugin */
	private String pluginId;

	/** Description of the sample */
	private String description;

	/** Plugin-relative sample root directory */
	private String root;

	/** Root-relative path to the source directory */
	private String sourcePath;

	/** Root-relative path to the configuratino directory */
	private String configPath;

	/** Config sets that must be added for the sample */
	private IMuleConfigSet[] configSets;

	/**
	 * Return the pluginId.
	 * 
	 * @return returns pluginId.
	 */
	public String getPluginId() {
		return pluginId;
	}

	/**
	 * Set the pluginId.
	 * 
	 * @param pluginId The pluginId.
	 */
	public void setPluginId(String pluginId) {
		this.pluginId = pluginId;
	}

	/**
	 * Return the configPath.
	 * 
	 * @return returns configPath.
	 */
	public String getConfigPath() {
		return configPath;
	}

	/**
	 * Set the configPath.
	 * 
	 * @param configPath The configPath.
	 */
	public void setConfigPath(String configPath) {
		this.configPath = configPath;
	}

	/**
	 * Return the description.
	 * 
	 * @return returns description.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Set the description.
	 * 
	 * @param description The description.
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Return the root.
	 * 
	 * @return returns root.
	 */
	public String getRoot() {
		return root;
	}

	/**
	 * Set the root.
	 * 
	 * @param root The root.
	 */
	public void setRoot(String root) {
		this.root = root;
	}

	/**
	 * Return the sourcePath.
	 * 
	 * @return returns sourcePath.
	 */
	public String getSourcePath() {
		return sourcePath;
	}

	/**
	 * Set the sourcePath.
	 * 
	 * @param sourcePath The sourcePath.
	 */
	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}

	/**
	 * Return the configSets.
	 * 
	 * @return returns configSets.
	 */
	public IMuleConfigSet[] getConfigSets() {
		return configSets;
	}

	/**
	 * Set the configSets.
	 * 
	 * @param configSets The configSets.
	 */
	public void setConfigSets(IMuleConfigSet[] configSets) {
		this.configSets = configSets;
	}
}