package org.mule.ide.core.samples;

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

	/** Config set data to be added for the sample */
	private ConfigSet[] configSets;

	/**
	 * Gets the configPath
	 * 
	 * @return returns the configPath
	 */
	public String getConfigPath() {
		return configPath;
	}

	/**
	 * Sets the configPath.
	 * 
	 * @param configPath
	 *            the configPath
	 */
	public void setConfigPath(String configPath) {
		this.configPath = configPath;
	}

	/**
	 * Gets the configSets
	 * 
	 * @return returns the configSets
	 */
	public ConfigSet[] getConfigSets() {
		return configSets;
	}

	/**
	 * Sets the configSets.
	 * 
	 * @param configSets
	 *            the configSets
	 */
	public void setConfigSets(ConfigSet[] configSets) {
		this.configSets = configSets;
	}

	/**
	 * Gets the description
	 * 
	 * @return returns the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the description.
	 * 
	 * @param description
	 *            the description
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Gets the pluginId
	 * 
	 * @return returns the pluginId
	 */
	public String getPluginId() {
		return pluginId;
	}

	/**
	 * Sets the pluginId.
	 * 
	 * @param pluginId
	 *            the pluginId
	 */
	public void setPluginId(String pluginId) {
		this.pluginId = pluginId;
	}

	/**
	 * Gets the root
	 * 
	 * @return returns the root
	 */
	public String getRoot() {
		return root;
	}

	/**
	 * Sets the root.
	 * 
	 * @param root
	 *            the root
	 */
	public void setRoot(String root) {
		this.root = root;
	}

	/**
	 * Gets the sourcePath
	 * 
	 * @return returns the sourcePath
	 */
	public String getSourcePath() {
		return sourcePath;
	}

	/**
	 * Sets the sourcePath.
	 * 
	 * @param sourcePath
	 *            the sourcePath
	 */
	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}

}