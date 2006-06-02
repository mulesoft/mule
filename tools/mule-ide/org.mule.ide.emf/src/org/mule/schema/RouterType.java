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
 * A representation of the model object '<em><b>Router Type</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.mule.schema.RouterType#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.RouterType#getEndpoint <em>Endpoint</em>}</li>
 *   <li>{@link org.mule.schema.RouterType#getGlobalEndpoint <em>Global Endpoint</em>}</li>
 *   <li>{@link org.mule.schema.RouterType#getReplyTo <em>Reply To</em>}</li>
 *   <li>{@link org.mule.schema.RouterType#getTransaction <em>Transaction</em>}</li>
 *   <li>{@link org.mule.schema.RouterType#getFilter <em>Filter</em>}</li>
 *   <li>{@link org.mule.schema.RouterType#getProperties <em>Properties</em>}</li>
 *   <li>{@link org.mule.schema.RouterType#getClassName <em>Class Name</em>}</li>
 *   <li>{@link org.mule.schema.RouterType#getEnableCorrelation <em>Enable Correlation</em>}</li>
 *   <li>{@link org.mule.schema.RouterType#getPropertyExtractor <em>Property Extractor</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.mule.schema.MulePackage#getRouterType()
 * @model extendedMetaData="name='routerType' kind='mixed'"
 * @generated
 */
public interface RouterType extends EObject {
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
	 * @see org.mule.schema.MulePackage#getRouterType_Mixed()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.EFeatureMapEntry" many="true"
	 *        extendedMetaData="kind='elementWildcard' name=':mixed'"
	 * @generated
	 */
	FeatureMap getMixed();

	/**
	 * Returns the value of the '<em><b>Endpoint</b></em>' containment reference list.
	 * The list contents are of type {@link org.mule.schema.EndpointType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Endpoint</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Endpoint</em>' containment reference list.
	 * @see org.mule.schema.MulePackage#getRouterType_Endpoint()
	 * @model type="org.mule.schema.EndpointType" containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='endpoint' namespace='##targetNamespace'"
	 * @generated
	 */
	EList getEndpoint();

	/**
	 * Returns the value of the '<em><b>Global Endpoint</b></em>' containment reference list.
	 * The list contents are of type {@link org.mule.schema.GlobalEndpointType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Global Endpoint</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Global Endpoint</em>' containment reference list.
	 * @see org.mule.schema.MulePackage#getRouterType_GlobalEndpoint()
	 * @model type="org.mule.schema.GlobalEndpointType" containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='global-endpoint' namespace='##targetNamespace'"
	 * @generated
	 */
	EList getGlobalEndpoint();

	/**
	 * Returns the value of the '<em><b>Reply To</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Reply To</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Reply To</em>' containment reference.
	 * @see #setReplyTo(ReplyToType)
	 * @see org.mule.schema.MulePackage#getRouterType_ReplyTo()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='reply-to' namespace='##targetNamespace'"
	 * @generated
	 */
	ReplyToType getReplyTo();

	/**
	 * Sets the value of the '{@link org.mule.schema.RouterType#getReplyTo <em>Reply To</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Reply To</em>' containment reference.
	 * @see #getReplyTo()
	 * @generated
	 */
	void setReplyTo(ReplyToType value);

	/**
	 * Returns the value of the '<em><b>Transaction</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Transaction</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Transaction</em>' containment reference.
	 * @see #setTransaction(TransactionType)
	 * @see org.mule.schema.MulePackage#getRouterType_Transaction()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='transaction' namespace='##targetNamespace'"
	 * @generated
	 */
	TransactionType getTransaction();

	/**
	 * Sets the value of the '{@link org.mule.schema.RouterType#getTransaction <em>Transaction</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Transaction</em>' containment reference.
	 * @see #getTransaction()
	 * @generated
	 */
	void setTransaction(TransactionType value);

	/**
	 * Returns the value of the '<em><b>Filter</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Filter</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Filter</em>' containment reference.
	 * @see #setFilter(FilterType)
	 * @see org.mule.schema.MulePackage#getRouterType_Filter()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='filter' namespace='##targetNamespace'"
	 * @generated
	 */
	FilterType getFilter();

	/**
	 * Sets the value of the '{@link org.mule.schema.RouterType#getFilter <em>Filter</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Filter</em>' containment reference.
	 * @see #getFilter()
	 * @generated
	 */
	void setFilter(FilterType value);

	/**
	 * Returns the value of the '<em><b>Properties</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Properties</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Properties</em>' containment reference.
	 * @see #setProperties(PropertiesType)
	 * @see org.mule.schema.MulePackage#getRouterType_Properties()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='properties' namespace='##targetNamespace'"
	 * @generated
	 */
	PropertiesType getProperties();

	/**
	 * Sets the value of the '{@link org.mule.schema.RouterType#getProperties <em>Properties</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Properties</em>' containment reference.
	 * @see #getProperties()
	 * @generated
	 */
	void setProperties(PropertiesType value);

	/**
	 * Returns the value of the '<em><b>Class Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Class Name</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Class Name</em>' attribute.
	 * @see #setClassName(String)
	 * @see org.mule.schema.MulePackage#getRouterType_ClassName()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String" required="true"
	 *        extendedMetaData="kind='attribute' name='className' namespace='##targetNamespace'"
	 * @generated
	 */
	String getClassName();

	/**
	 * Sets the value of the '{@link org.mule.schema.RouterType#getClassName <em>Class Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Class Name</em>' attribute.
	 * @see #getClassName()
	 * @generated
	 */
	void setClassName(String value);

	/**
	 * Returns the value of the '<em><b>Enable Correlation</b></em>' attribute.
	 * The default value is <code>"IF_NOT_SET"</code>.
	 * The literals are from the enumeration {@link org.mule.schema.EnableCorrelationType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Enable Correlation</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Enable Correlation</em>' attribute.
	 * @see org.mule.schema.EnableCorrelationType
	 * @see #isSetEnableCorrelation()
	 * @see #unsetEnableCorrelation()
	 * @see #setEnableCorrelation(EnableCorrelationType)
	 * @see org.mule.schema.MulePackage#getRouterType_EnableCorrelation()
	 * @model default="IF_NOT_SET" unique="false" unsettable="true"
	 *        extendedMetaData="kind='attribute' name='enableCorrelation' namespace='##targetNamespace'"
	 * @generated
	 */
	EnableCorrelationType getEnableCorrelation();

	/**
	 * Sets the value of the '{@link org.mule.schema.RouterType#getEnableCorrelation <em>Enable Correlation</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Enable Correlation</em>' attribute.
	 * @see org.mule.schema.EnableCorrelationType
	 * @see #isSetEnableCorrelation()
	 * @see #unsetEnableCorrelation()
	 * @see #getEnableCorrelation()
	 * @generated
	 */
	void setEnableCorrelation(EnableCorrelationType value);

	/**
	 * Unsets the value of the '{@link org.mule.schema.RouterType#getEnableCorrelation <em>Enable Correlation</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSetEnableCorrelation()
	 * @see #getEnableCorrelation()
	 * @see #setEnableCorrelation(EnableCorrelationType)
	 * @generated
	 */
	void unsetEnableCorrelation();

	/**
	 * Returns whether the value of the '{@link org.mule.schema.RouterType#getEnableCorrelation <em>Enable Correlation</em>}' attribute is set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return whether the value of the '<em>Enable Correlation</em>' attribute is set.
	 * @see #unsetEnableCorrelation()
	 * @see #getEnableCorrelation()
	 * @see #setEnableCorrelation(EnableCorrelationType)
	 * @generated
	 */
	boolean isSetEnableCorrelation();

	/**
	 * Returns the value of the '<em><b>Property Extractor</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Property Extractor</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Property Extractor</em>' attribute.
	 * @see #setPropertyExtractor(String)
	 * @see org.mule.schema.MulePackage#getRouterType_PropertyExtractor()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='propertyExtractor' namespace='##targetNamespace'"
	 * @generated
	 */
	String getPropertyExtractor();

	/**
	 * Sets the value of the '{@link org.mule.schema.RouterType#getPropertyExtractor <em>Property Extractor</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Property Extractor</em>' attribute.
	 * @see #getPropertyExtractor()
	 * @generated
	 */
	void setPropertyExtractor(String value);

} // RouterType
