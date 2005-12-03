/*
 * $Header$
 * $Revision$
 * $Date$
 */
package org.mule.ide.internal.core.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.resource.Resource;
import org.mule.ide.ConfigFileType;
import org.mule.ide.DocumentRoot;
import org.mule.ide.MuleIDEFactory;
import org.mule.ide.MuleIdeConfigType;
import org.mule.ide.core.IMuleDefaults;
import org.mule.ide.core.MuleCorePlugin;
import org.mule.ide.core.model.IMuleConfigSet;
import org.mule.ide.core.model.IMuleConfiguration;
import org.mule.ide.core.model.IMuleModel;
import org.mule.ide.util.MuleIDEResourceFactoryImpl;

/**
 * Default Mule model implementation.
 */
public class MuleModel extends MuleModelElement implements IMuleModel {

	/** Map of Mule configurations hashed by unique id */
	private Map muleConfigurations = new HashMap();

	/** Map of Mule config sets hashed by unique id */
	private Map muleConfigSets = new HashMap();

	/** The project this model belongs to */
	private IProject project;

	/** Error message for marker when config file can not be read */
	private static final String ERROR_READING_CONFIG_FILE = "Could not read Mule IDE configuration file.";

	/** Error message for marker when config file elements can not be loaded */
	private static final String ERROR_LOADING_CONFIGS = "One or more configuration elements could not be loaded.";

	/**
	 * Create a Mule IDE model for the given project.
	 * 
	 * @param project the project
	 */
	public MuleModel(IProject project) {
		this.project = project;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleModel#createNewMuleConfiguration(java.lang.String,
	 * java.lang.String)
	 */
	public IMuleConfiguration createNewMuleConfiguration(String description, String relativePath) {
		return new MuleConfiguration(this, getUniqueConfigurationId(), description, relativePath);
	}

	/**
	 * Find a unique id for a configuration within this model.
	 * 
	 * @return the unique id
	 */
	protected String getUniqueConfigurationId() {
		String base = "config";
		int index = 0;
		while (true) {
			String id = base + String.valueOf(index);
			if (getMuleConfiguration(id) == null) {
				return id;
			}
			index++;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleModel#createNewMuleConfigSet(java.lang.String)
	 */
	public IMuleConfigSet createNewMuleConfigSet(String description) {
		return new MuleConfigSet(this, getUniqueConfigSetId(), description);
	}

	/**
	 * Find a unique id for a config set within this model.
	 * 
	 * @return the unique id
	 */
	protected String getUniqueConfigSetId() {
		String base = "set";
		int index = 0;
		while (true) {
			String id = base + String.valueOf(index);
			if (getMuleConfigSet(id) == null) {
				return id;
			}
			index++;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleModel#getMuleConfigurations()
	 */
	public Collection getMuleConfigurations() {
		return muleConfigurations.values();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleModel#getMuleConfigurationForPath(org.eclipse.core.runtime.IPath)
	 */
	public IMuleConfiguration getMuleConfigurationForPath(IPath path) {
		Iterator it = getMuleConfigurations().iterator();
		while (it.hasNext()) {
			IMuleConfiguration config = (IMuleConfiguration) it.next();
			if (path.equals(config.getFilePath())) {
				return config;
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleModel#addMuleConfiguration(org.mule.ide.core.model.IMuleConfiguration)
	 */
	public void addMuleConfiguration(IMuleConfiguration muleConfiguration) {
		if (muleConfiguration == null) {
			throw new IllegalArgumentException("Attempted to add a null configuration.");
		}
		synchronized (this.muleConfigurations) {
			this.muleConfigurations.put(muleConfiguration.getId(), muleConfiguration);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleModel#getMuleConfiguration(java.lang.String)
	 */
	public IMuleConfiguration getMuleConfiguration(String id) {
		return (IMuleConfiguration) this.muleConfigurations.get(id);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleModel#removeMuleConfiguration(org.mule.ide.core.model.IMuleConfiguration)
	 */
	public IMuleConfiguration removeMuleConfiguration(String id) {
		if (id == null) {
			throw new IllegalArgumentException("Null id passed in removeMuleConfiguration.");
		}
		synchronized (this.muleConfigurations) {
			return (IMuleConfiguration) this.muleConfigurations.remove(id);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleModel#getMuleConfigSets()
	 */
	public Collection getMuleConfigSets() {
		return muleConfigSets.values();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleModel#addMuleConfigSet(org.mule.ide.core.model.IMuleConfigSet)
	 */
	public void addMuleConfigSet(IMuleConfigSet muleConfigSet) {
		if (muleConfigSet == null) {
			throw new IllegalArgumentException("Attempted to add a null config set.");
		}
		synchronized (this.muleConfigSets) {
			this.muleConfigSets.put(muleConfigSet.getId(), muleConfigSet);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleModel#getMuleConfigSet(java.lang.String)
	 */
	public IMuleConfigSet getMuleConfigSet(String id) {
		return (IMuleConfigSet) this.muleConfigSets.get(id);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleModel#removeMuleConfigSet(java.lang.String)
	 */
	public IMuleConfigSet removeMuleConfigSet(String id) {
		if (id == null) {
			throw new IllegalArgumentException("Null id passed in removeMuleConfigSet.");
		}
		synchronized (this.muleConfigurations) {
			return (IMuleConfigSet) this.muleConfigSets.remove(id);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleModelElement#refresh()
	 */
	public IStatus refresh() {
		setStatus(Status.OK_STATUS);
		IFile file = getProject().getFile(IMuleDefaults.MULE_IDE_CONFIG_FILENAME);
		if (file.exists()) {
			try {
				Resource resource = (new MuleIDEResourceFactoryImpl()).createResource(null);
				resource.load(file.getContents(), Collections.EMPTY_MAP);
				if (!resource.getContents().isEmpty()) {
					DocumentRoot root = (DocumentRoot) resource.getContents().get(0);
					MuleIdeConfigType config = root.getMuleIdeConfig();
					if (config != null) {
						setStatus(loadFrom(config));
						return getStatus();
					}
				}
			} catch (Exception e) {
				MuleCorePlugin.getDefault().logException(ERROR_READING_CONFIG_FILE, e);
				setStatus(MuleCorePlugin.getDefault().createErrorStatus(ERROR_READING_CONFIG_FILE,
						e));
				return getStatus();
			}
		}
		setStatus(MuleCorePlugin.getDefault().createErrorStatus(ERROR_READING_CONFIG_FILE, null));
		return getStatus();
	}

	/**
	 * Load the Eclipse model from the underlying EMF representation.
	 * 
	 * @param emfModel the EMF model
	 * @return a status indicator
	 */
	public IStatus loadFrom(MuleIdeConfigType emfModel) {
		MultiStatus multi = MuleCorePlugin.getDefault().createMultiStatus(ERROR_LOADING_CONFIGS);

		// Convert the config files.
		EList configFiles = emfModel.getConfigFile();
		Iterator it = configFiles.iterator();
		while (it.hasNext()) {
			ConfigFileType configFile = (ConfigFileType) it.next();
			IMuleConfiguration modelConfig = MuleModelFactory.convert(this, configFile);
			IStatus status = modelConfig.refresh();
			addMuleConfiguration(modelConfig);

			// Store any errors or warnings.
			if (!status.isOK()) {
				multi.add(status);
			}
		}
		return multi;
	}

	/**
	 * Saves the Eclipse model to the EMF model for peristence.
	 * 
	 * @param emfModel the EMF model
	 */
	public IStatus saveTo(MuleIdeConfigType emfModel) {
		List configFiles = new ArrayList(getMuleConfigurations());
		Collections.sort(configFiles);
		Iterator it = configFiles.iterator();
		while (it.hasNext()) {
			IMuleConfiguration configFile = (IMuleConfiguration) it.next();
			emfModel.getConfigFile().add(MuleModelFactory.convert(configFile));
		}
		return Status.OK_STATUS;
	}

	/**
	 * Converts the Eclipse model to XML via the EMF model.
	 * 
	 * @return the XML
	 * @throws IOException
	 */
	public String getAsXML() throws IOException {
		Resource resource = (new MuleIDEResourceFactoryImpl()).createResource(null);
		DocumentRoot root = MuleIDEFactory.eINSTANCE.createDocumentRoot();
		MuleIdeConfigType config = MuleIDEFactory.eINSTANCE.createMuleIdeConfigType();
		saveTo(config);
		root.setMuleIdeConfig(config);
		resource.getContents().add(root);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		resource.save(output, Collections.EMPTY_MAP);
		return new String(output.toByteArray());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleModel#getProject()
	 */
	public IProject getProject() {
		return this.project;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.model.IMuleModelElement#getMuleModel()
	 */
	public IMuleModel getMuleModel() {
		return this;
	}
}