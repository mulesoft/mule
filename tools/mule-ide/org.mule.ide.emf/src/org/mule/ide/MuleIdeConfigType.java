/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.mule.ide;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Mule Ide Config Type</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * Holds all configuration info.
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.mule.ide.MuleIdeConfigType#getConfigFile <em>Config File</em>}</li>
 *   <li>{@link org.mule.ide.MuleIdeConfigType#getConfigSet <em>Config Set</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.mule.ide.MuleIDEPackage#getMuleIdeConfigType()
 * @model extendedMetaData="name='mule-ide-config-type' kind='elementOnly'"
 * @generated
 */
public interface MuleIdeConfigType extends EObject {
	/**
	 * Returns the value of the '<em><b>Config File</b></em>' containment reference list.
	 * The list contents are of type {@link org.mule.ide.ConfigFileType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Config File</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Config File</em>' containment reference list.
	 * @see org.mule.ide.MuleIDEPackage#getMuleIdeConfigType_ConfigFile()
	 * @model type="org.mule.ide.ConfigFileType" containment="true" resolveProxies="false"
	 *        extendedMetaData="kind='element' name='config-file' namespace='##targetNamespace'"
	 * @generated
	 */
	EList getConfigFile();

	/**
	 * Returns the value of the '<em><b>Config Set</b></em>' containment reference list.
	 * The list contents are of type {@link org.mule.ide.ConfigSetType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Config Set</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Config Set</em>' containment reference list.
	 * @see org.mule.ide.MuleIDEPackage#getMuleIdeConfigType_ConfigSet()
	 * @model type="org.mule.ide.ConfigSetType" containment="true" resolveProxies="false"
	 *        extendedMetaData="kind='element' name='config-set' namespace='##targetNamespace'"
	 * @generated
	 */
	EList getConfigSet();

} // MuleIdeConfigType
