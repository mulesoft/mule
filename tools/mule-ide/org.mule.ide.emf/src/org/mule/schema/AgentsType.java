/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.mule.schema;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

import org.eclipse.emf.ecore.util.FeatureMap;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Agents Type</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.mule.schema.AgentsType#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.AgentsType#getAgent <em>Agent</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.mule.schema.MulePackage#getAgentsType()
 * @model extendedMetaData="name='agentsType' kind='mixed'"
 * @generated
 */
public interface AgentsType extends EObject {
    /**
     * Returns the value of the '<em><b>Mixed</b></em>' attribute list.
     * The list contents are of type {@link org.eclipse.emf.ecore.util.FeatureMap.Entry}.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Mixed</em>' attribute list isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @return the value of the '<em>Mixed</em>' attribute list.
     * @see org.mule.schema.MulePackage#getAgentsType_Mixed()
     * @model unique="false" dataType="org.eclipse.emf.ecore.EFeatureMapEntry" many="true"
     *        extendedMetaData="kind='elementWildcard' name=':mixed'"
     * @generated
     */
    FeatureMap getMixed();

    /**
     * Returns the value of the '<em><b>Agent</b></em>' containment reference list.
     * The list contents are of type {@link org.mule.schema.AgentType}.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Agent</em>' containment reference list isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * @return the value of the '<em>Agent</em>' containment reference list.
     * @see org.mule.schema.MulePackage#getAgentsType_Agent()
     * @model type="org.mule.schema.AgentType" containment="true" resolveProxies="false" required="true" transient="true" volatile="true" derived="true"
     *        extendedMetaData="kind='element' name='agent' namespace='##targetNamespace'"
     * @generated
     */
    EList getAgent();

} // AgentsType
