/*
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (C) Alkacon Software (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.ade.configuration;

import org.opencms.file.CmsObject;
import org.opencms.file.CmsProperty;
import org.opencms.file.CmsRequestContext;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.file.CmsVfsResourceAlreadyExistsException;
import org.opencms.file.CmsVfsResourceNotFoundException;
import org.opencms.file.types.CmsResourceTypeFolder;
import org.opencms.file.types.I_CmsResourceType;
import org.opencms.lock.CmsLock;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.main.OpenCms;
import org.opencms.security.CmsPermissionSet;
import org.opencms.security.CmsRole;
import org.opencms.util.CmsStringUtil;
import org.opencms.workplace.explorer.CmsExplorerTypeSettings;
import org.opencms.xml.containerpage.CmsFormatterConfiguration;
import org.opencms.xml.containerpage.CmsXmlDynamicFunctionHandler;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;

import com.google.common.collect.Lists;

/**
 * The configuration for a single resource type.<p>
 */
public class CmsResourceTypeConfig implements I_CmsConfigurationObject<CmsResourceTypeConfig> {

    /** The log instance for this class. */
    private static final Log LOG = CmsLog.getLog(CmsResourceTypeConfig.class);

    /** The CMS object used for VFS operations. */
    protected CmsObject m_cms;

    /** The flag for disabling detail pages. */
    private boolean m_detailPagesDisabled;

    /** True if this is a disabled configuration. */
    private boolean m_disabled;

    /** A reference to a folder of folder name. */
    private CmsFolderOrName m_folderOrName;

    /** The formatter configuration. */
    private CmsFormatterConfiguration m_formatterConfig;

    /** The name pattern .*/
    private String m_namePattern;

    /** The number used for sorting the resource type configurations. */
    private int m_order;

    /** The name of the resource type. */
    private String m_typeName;

    /** 
     * Creates a new resource type configuration.<p>
     * 
     * @param typeName the resource type name 
     * @param disabled true if this is a disabled configuration 
     * @param folder the folder reference 
     * @param pattern the name pattern 
     * @param formatterConfig the formatter configuration 
     */
    public CmsResourceTypeConfig(
        String typeName,
        boolean disabled,
        CmsFolderOrName folder,
        String pattern,
        CmsFormatterConfiguration formatterConfig) {

        this(typeName, disabled, folder, pattern, formatterConfig, false, I_CmsConfigurationObject.DEFAULT_ORDER);
    }

    /** 
     * Creates a new resource type configuration.<p>
     * 
     * @param typeName the resource type name 
     * @param disabled true if this is a disabled configuration 
     * @param folder the folder reference 
     * @param pattern the name pattern 
     * @param formatterConfig the formatter configuration 
     * @param detailPagesDisabled true if detail page creation should be disabled for this type
     * @param order the number used for sorting resource types from modules  
     */
    public CmsResourceTypeConfig(
        String typeName,
        boolean disabled,
        CmsFolderOrName folder,
        String pattern,
        CmsFormatterConfiguration formatterConfig,
        boolean detailPagesDisabled,
        int order) {

        m_typeName = typeName;
        m_disabled = disabled;
        m_folderOrName = folder;
        m_namePattern = pattern;
        m_formatterConfig = formatterConfig;
        m_detailPagesDisabled = detailPagesDisabled;
        m_order = order;
    }

    /** 
     * Creates a new resource type configuration.<p>
     * 
     * @param typeName the resource type name 
     * @param disabled true if this is a disabled configuration 
     * @param folder the folder reference 
     * @param pattern the name pattern 
     * @param formatterConfig the formatter configuration 
     * @param order the number used for sorting resource types from modules 
     */
    public CmsResourceTypeConfig(
        String typeName,
        boolean disabled,
        CmsFolderOrName folder,
        String pattern,
        CmsFormatterConfiguration formatterConfig,
        int order) {

        this(typeName, disabled, folder, pattern, formatterConfig, false, order);
    }

    /** 
     * Checks if this resource type is creatable.<p>
     * 
     * @param cms the current CMS context 
     * 
     * @return <code>true</code> if the resource type is creatable 
     * 
     * @throws CmsException if something goes wrong 
     */
    public boolean checkCreatable(CmsObject cms) throws CmsException {

        if (cms.getRequestContext().getCurrentProject().isOnlineProject()
            || "".equals(cms.getRequestContext().getSiteRoot())) {
            return false;
        }
        if (CmsXmlDynamicFunctionHandler.TYPE_FUNCTION.equals(m_typeName)) {
            return OpenCms.getRoleManager().hasRole(cms, CmsRole.DEVELOPER);
        }
        checkInitialized();
        String folderPath = getFolderPath(cms);
        CmsObject createCms = OpenCms.initCmsObject(m_cms);
        createCms.getRequestContext().setCurrentProject(cms.getRequestContext().getCurrentProject());
        String oldSiteRoot = cms.getRequestContext().getSiteRoot();
        cms.getRequestContext().setSiteRoot("");
        tryToUnlock(cms, folderPath);
        createFolder(createCms, folderPath);
        try {
            CmsResource permissionCheckFolder = cms.readResource(folderPath);
            CmsExplorerTypeSettings settings = OpenCms.getWorkplaceManager().getExplorerTypeSetting(m_typeName);
            boolean editable = settings.isEditable(cms, permissionCheckFolder);
            boolean controlPermission = settings.getAccess().getPermissions(cms, permissionCheckFolder).requiresControlPermission();
            boolean hasWritePermission = cms.hasPermissions(
                permissionCheckFolder,
                CmsPermissionSet.ACCESS_WRITE,
                false,
                CmsResourceFilter.ONLY_VISIBLE_NO_DELETED);
            return editable && controlPermission && hasWritePermission;
        } catch (CmsVfsResourceNotFoundException e) {
            return false;
        } catch (CmsException e) {
            LOG.error(e.getLocalizedMessage(), e);
            return false;
        } finally {
            cms.getRequestContext().setSiteRoot(oldSiteRoot);
        }
    }

    /**
     * Checks if a resource type is viewable for the current user. 
     * If not, this resource type should not be available at all within the ADE 'add-wizard'.<p>
     * 
     * @param cms the current CMS context 
     * @param referenceUri the resource URI to check permissions for
     * 
     * @return <code>true</code> if the resource type is viewable 
     */
    public boolean checkViewable(CmsObject cms, String referenceUri) {

        try {
            CmsExplorerTypeSettings settings = OpenCms.getWorkplaceManager().getExplorerTypeSetting(m_typeName);
            CmsResource siteRoot = cms.readResource(referenceUri);
            return settings.getAccess().getPermissions(cms, siteRoot).requiresViewPermission();
        } catch (CmsException e) {
            LOG.error(e.getLocalizedMessage(), e);
            return false;
        }
    }

    /**
     * Checks whether the object is initialized and throws an exception otherwise.<p>
    */
    public void checkInitialized() {

        if (m_cms == null) {
            throw new IllegalStateException();
        }
    }

    /**
     * Checks whether the cms context is in the offline project and throws an exception otherwise.<p>
     * @param cms
     */
    public void checkOffline(CmsObject cms) {

        if (cms.getRequestContext().getCurrentProject().isOnlineProject()) {
            throw new IllegalStateException();
        }
    }

    /**
     * Creates a folder and its parent folders if they don't exist.<p>
     * 
     * @param cms the CMS context to use 
     * @param rootPath the folder root path 
     * 
     * @throws CmsException if something goes wrong 
     */
    public void createFolder(CmsObject cms, String rootPath) throws CmsException {

        cms.getRequestContext().setSiteRoot("");
        List<String> parents = new ArrayList<String>();
        String currentPath = rootPath;
        while (currentPath != null) {
            if (cms.existsResource(currentPath)) {
                break;
            }
            parents.add(currentPath);
            currentPath = CmsResource.getParentFolder(currentPath);
        }
        parents = Lists.reverse(parents);
        for (String parent : parents) {
            try {
                cms.createResource(parent, CmsResourceTypeFolder.getStaticTypeId());
                try {
                    cms.unlockResource(parent);
                } catch (CmsException e) {
                    // may happen if parent folder is locked also
                    if (LOG.isInfoEnabled()) {
                        LOG.info(e);
                    }
                }
            } catch (CmsVfsResourceAlreadyExistsException e) {
                // nop 
            }
        }
    }

    /**
     * Creates a new element.<p>
     * 
     * @param userCms the CMS context to use
     * 
     * @return the created resource
     *  
     * @throws CmsException if something goes wrong 
     */
    public CmsResource createNewElement(CmsObject userCms) throws CmsException {

        return createNewElement(userCms, null);
    }

    /**
     * Creates a new element.<p>
     * 
     * @param userCms the CMS context to use
     * @param modelResource the model resource to use
     * 
     * @return the created resource
     *  
     * @throws CmsException if something goes wrong 
     */
    public CmsResource createNewElement(CmsObject userCms, CmsResource modelResource) throws CmsException {

        checkOffline(userCms);
        checkInitialized();
        CmsObject rootCms = rootCms(userCms);
        String folderPath = getFolderPath(userCms);
        createFolder(m_cms, folderPath);
        String destination = CmsStringUtil.joinPaths(folderPath, getNamePattern(true));
        String creationPath = OpenCms.getResourceManager().getNameGenerator().getNewFileName(rootCms, destination, 5);
        // set the content locale
        rootCms.getRequestContext().setAttribute(
            CmsRequestContext.ATTRIBUTE_NEW_RESOURCE_LOCALE,
            userCms.getRequestContext().getLocale());
        if (modelResource != null) {
            // set the model resource
            rootCms.getRequestContext().setAttribute(CmsRequestContext.ATTRIBUTE_MODEL, modelResource.getRootPath());
        }
        CmsResource createdResource = rootCms.createResource(
            creationPath,
            getType().getTypeId(),
            null,
            new ArrayList<CmsProperty>(0));
        return createdResource;
    }

    /**
     * Computes the folder path for this resource type.<p>
     * 
     * @param cms the cms context to use 
     * 
     * @return the folder root path for this resource type 
     */
    public String getFolderPath(CmsObject cms) {

        checkInitialized();
        if (m_folderOrName != null) {
            return m_folderOrName.getFolderPath(cms);
        } else {
            return CmsStringUtil.joinPaths(
                cms.getRequestContext().getSiteRoot(),
                CmsADEConfigData.CONTENT_FOLDER_NAME,
                m_typeName);
        }
    }

    /**
     * @see org.opencms.ade.configuration.I_CmsConfigurationObject#getKey()
     */
    public String getKey() {

        return m_typeName;
    }

    /**
     * Gets the name pattern.<p> 
     * 
     * @param useDefaultIfEmpty if true, uses a default value if the name pattern isn't set directly
     *  
     * @return the name pattern 
     */
    public String getNamePattern(boolean useDefaultIfEmpty) {

        if (m_namePattern != null) {
            return m_namePattern;
        }
        if (useDefaultIfEmpty) {
            return m_typeName + "-%(number).html";
        }
        return null;
    }

    /**
     * Returns the number used for sorting module resource types.<p>
     * 
     * @return the number used for sorting module resource types 
     */
    public int getOrder() {

        return m_order;
    }

    /**
     * Gets the actual resource type for which this is a configuration.<p>
     * 
     * @return the actual resource type
     *  
     * @throws CmsException if something goes wrong 
     */
    public I_CmsResourceType getType() throws CmsException {

        return OpenCms.getResourceManager().getResourceType(m_typeName);
    }

    /**
     * Returns the type name.<p>
     * 
     * @return the type name 
     */
    public String getTypeName() {

        return m_typeName;
    }

    /**
     * Initializes this instance.<p>
     * 
     * @param cms the CMS context to use  
     */
    public void initialize(CmsObject cms) {

        m_cms = cms;

    }

    /**
     * True if the detail page creation should be disabled for this resource type.<p>
     * 
     * @return true if detail page creation should be disabled for this type 
     */
    public boolean isDetailPagesDisabled() {

        return m_detailPagesDisabled;
    }

    /**
     * @see org.opencms.ade.configuration.I_CmsConfigurationObject#isDisabled()
     */
    public boolean isDisabled() {

        return m_disabled;
    }

    /**
     * @see org.opencms.ade.configuration.I_CmsConfigurationObject#merge(org.opencms.ade.configuration.I_CmsConfigurationObject)
     */
    public CmsResourceTypeConfig merge(CmsResourceTypeConfig childConfig) {

        CmsFolderOrName folderOrName = childConfig.m_folderOrName != null ? childConfig.m_folderOrName : m_folderOrName;
        String namePattern = childConfig.m_namePattern != null ? childConfig.m_namePattern : m_namePattern;
        CmsFormatterConfiguration formatterConfig = childConfig.m_formatterConfig != null
        ? childConfig.m_formatterConfig
        : m_formatterConfig;
        return new CmsResourceTypeConfig(
            m_typeName,
            false,
            folderOrName,
            namePattern,
            formatterConfig,
            isDetailPagesDisabled() || childConfig.isDetailPagesDisabled(),
            m_order);

    }

    /**
     * Creates a shallow copy of this resource type configuration object.<p>
     * 
     * @return a copy of the resource type configuration object 
     */
    protected CmsResourceTypeConfig copy() {

        return new CmsResourceTypeConfig(
            m_typeName,
            m_disabled,
            getFolderOrName(),
            m_namePattern,
            m_formatterConfig,
            m_detailPagesDisabled,
            m_order);
    }

    /**
     * Returns the folder bean from the configuration.<p>
     * 
     * Normally, you should use getFolderPath() instead.<p>
     * 
     * @return the folder bean from the configuration 
     */
    protected CmsFolderOrName getFolderOrName() {

        return m_folderOrName;
    }

    /**
     * Gets the formatter configuration of this resource type.<p>
     * 
     * @return the formatter configuration of this resource type 
     */
    protected CmsFormatterConfiguration getFormatterConfiguration() {

        return m_formatterConfig;
    }

    /**
     * Gets the configured name pattern.<p>
     * 
     * @return the configured name pattern 
     */
    protected String getNamePattern() {

        return m_namePattern;
    }

    /**
     * Creates a new CMS object based on existing one and changes its site root to the site root.<p>
     * 
     * @param cms the CMS context 
     * @return the root site CMS context 
     * @throws CmsException if something goes wrong 
     */
    protected CmsObject rootCms(CmsObject cms) throws CmsException {

        CmsObject result = OpenCms.initCmsObject(cms);
        result.getRequestContext().setSiteRoot("");
        return result;
    }

    /**
     * Tries to remove a lock on an ancestor of a given path owned by the current user.<p>
     * 
     * @param cms the CMS context 
     * @param folderPath the path for which the lock should be removed 
     * 
     * @throws CmsException if something goes wrong 
     */
    protected void tryToUnlock(CmsObject cms, String folderPath) throws CmsException {

        // Get path of first ancestor that actually exists 
        while (!cms.existsResource(folderPath)) {
            folderPath = CmsResource.getParentFolder(folderPath);
        }
        CmsResource resource = cms.readResource(folderPath);
        CmsLock lock = cms.getLock(resource);
        // we are only interested in locks we can safely unlock, i.e. locks by the current user  
        if (lock.isOwnedBy(cms.getRequestContext().getCurrentUser())) {
            // walk up the tree until we get to the location from which the lock is inherited 
            while (lock.isInherited()) {
                folderPath = CmsResource.getParentFolder(folderPath);
                resource = cms.readResource(folderPath);
                lock = cms.getLock(resource);
            }
            cms.unlockResource(folderPath);
        }
    }

    /**
     * Updates the base path for the folder information.<p>
     * 
     * @param basePath the new base path 
     */
    protected void updateBasePath(String basePath) {

        if (m_folderOrName != null) {
            if (m_folderOrName.isName()) {
                m_folderOrName = new CmsFolderOrName(basePath, m_folderOrName.getFolderName());
            }
        } else {
            m_folderOrName = new CmsFolderOrName(basePath, m_typeName);
        }
    }
}
