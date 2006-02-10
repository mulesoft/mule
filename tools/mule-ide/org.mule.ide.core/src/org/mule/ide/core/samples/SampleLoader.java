package org.mule.ide.core.samples;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.mule.ide.core.MuleCorePlugin;

/**
 * Singleton that loads all samples contributed to the org.mule.ide.samples extension point.
 * 
 * @author Derek Adams
 */
public class SampleLoader {

	/** Singleton instance */
	private static SampleLoader instance;

	/** The samples loaded from contributing plugins */
	private Sample[] samples;

	/** Extension point from which samples are loaded */
	private static final String SAMPLES_EXTPOINT = "samples";

	/** Description attribute for a sample */
	private static final String SAMPLE_ATTR_DESC = "description";

	/** Root directory attribute for a sample */
	private static final String SAMPLE_ATTR_ROOT = "root";

	/** Source path attribute for a sample */
	private static final String SAMPLE_ATTR_SOURCE_DIR = "srcdir";

	/** Configuration path attribute for a sample */
	private static final String SAMPLE_ATTR_CONFIG_DIR = "confdir";

	/** Sub element for a config set associated with the sample */
	private static final String SAMPLE_ELM_CONFIGSET = "configset";

	/** Name attribute for a config set element */
	private static final String CONFIGSET_ATTR_NAME = "name";

	/** Config path attribute for a config set element */
	private static final String CONFIGSET_ATTR_CONFIGPATH = "configpath";

	/** Config name attribute for a config set element */
	private static final String CONFIGSET_ATTR_CONFIGNAME = "configname";

	/**
	 * Private singleton constructor.
	 */
	private SampleLoader() {
		setSamples(loadSamples());
	}

	/**
	 * Get the singleton instance
	 * 
	 * @return
	 */
	public static SampleLoader getInstance() {
		if (instance == null) {
			instance = new SampleLoader();
		}
		return instance;
	}

	/**
	 * Load the samples from contributing plugins.
	 * 
	 * @return the loaded samples
	 */
	protected Sample[] loadSamples() {
		IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(
				MuleCorePlugin.PLUGIN_ID, SAMPLES_EXTPOINT);
		IExtension[] extensions = extensionPoint.getExtensions();
		List samples = new ArrayList();
		for (int i = 0; i < extensions.length; i++) {
			IExtension extension = extensions[i];
			IConfigurationElement[] elements = extension.getConfigurationElements();
			for (int j = 0; j < elements.length; j++) {
				IConfigurationElement element = elements[j];
				Sample sample = new Sample();
				sample.setPluginId(extension.getNamespace());
				sample.setDescription(element.getAttribute(SAMPLE_ATTR_DESC));
				sample.setRoot(element.getAttribute(SAMPLE_ATTR_ROOT));
				sample.setSourcePath(element.getAttribute(SAMPLE_ATTR_SOURCE_DIR));
				sample.setConfigPath(element.getAttribute(SAMPLE_ATTR_CONFIG_DIR));
				sample.setConfigSets(createConfigSets(element.getChildren(SAMPLE_ELM_CONFIGSET)));
				samples.add(sample);
			}
		}
		return (Sample[]) samples.toArray(new Sample[samples.size()]);
	}

	/**
	 * Create config set data elements for all of the configset elements.
	 * 
	 * @param configElements the configuration elements
	 * @return the config set data objects
	 */
	protected ConfigSet[] createConfigSets(IConfigurationElement[] configElements) {
		List configSets = new ArrayList();
		for (int i = 0; i < configElements.length; i++) {
			ConfigSet set = new ConfigSet();
			set.setName(configElements[i].getAttribute(CONFIGSET_ATTR_NAME));
			set.setConfigPath(configElements[i].getAttribute(CONFIGSET_ATTR_CONFIGPATH));
			set.setConfigName(configElements[i].getAttribute(CONFIGSET_ATTR_CONFIGNAME));
			configSets.add(set);
		}
		return (ConfigSet[]) configSets.toArray(new ConfigSet[configSets.size()]);
	}

	/**
	 * Get the list of sample descriptions.
	 * 
	 * @return
	 */
	public String[] getSampleDescriptions() {
		Sample[] samples = getSamples();
		String[] result = new String[samples.length];
		for (int i = 0; i < samples.length; i++) {
			result[i] = samples[i].getDescription();
		}
		return result;
	}

	/**
	 * Get a Sample given its description.
	 * 
	 * @param description the description
	 * @return the sample or null if no match
	 */
	public Sample getSampleByDescription(String description) {
		Sample[] samples = getSamples();
		for (int i = 0; i < samples.length; i++) {
			if (samples[i].getDescription().equals(description)) {
				return samples[i];
			}
		}
		return null;
	}

	/**
	 * Return the samples.
	 * 
	 * @return returns samples.
	 */
	public Sample[] getSamples() {
		return samples;
	}

	/**
	 * Set the samples.
	 * 
	 * @param samples The samples.
	 */
	public void setSamples(Sample[] samples) {
		this.samples = samples;
	}
}