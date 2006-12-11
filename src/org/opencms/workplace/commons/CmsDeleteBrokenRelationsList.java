/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/commons/CmsDeleteBrokenRelationsList.java,v $
 * Date   : $Date: 2006/12/11 15:10:52 $
 * Version: $Revision: 1.1.2.8 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (c) 2005 Alkacon Software GmbH (http://www.alkacon.com)
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
 * For further information about Alkacon Software GmbH, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.workplace.commons;

import org.opencms.db.CmsUserSettings;
import org.opencms.file.CmsResource;
import org.opencms.jsp.CmsJspActionElement;
import org.opencms.main.CmsException;
import org.opencms.relations.CmsRelation;
import org.opencms.relations.CmsRelationDeleteValidator;
import org.opencms.relations.CmsRelationValidatorInfoEntry;
import org.opencms.site.CmsSiteManager;
import org.opencms.util.CmsStringUtil;
import org.opencms.workplace.CmsWorkplace;
import org.opencms.workplace.explorer.CmsResourceUtil;
import org.opencms.workplace.list.A_CmsListDialog;
import org.opencms.workplace.list.A_CmsListExplorerDialog;
import org.opencms.workplace.list.A_CmsListIndependentJsAction;
import org.opencms.workplace.list.CmsListColumnDefinition;
import org.opencms.workplace.list.CmsListIndependentAction;
import org.opencms.workplace.list.CmsListItem;
import org.opencms.workplace.list.CmsListItemDetails;
import org.opencms.workplace.list.CmsListItemDetailsFormatter;
import org.opencms.workplace.list.CmsListMetadata;
import org.opencms.workplace.list.I_CmsListAction;
import org.opencms.workplace.list.I_CmsListResourceCollector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * List for resources with links that could get broken after deletion.<p>
 * 
 * @author Michael Moossen  
 * 
 * @version $Revision: 1.1.2.8 $ 
 * 
 * @since 6.5.4 
 */
public class CmsDeleteBrokenRelationsList extends A_CmsListExplorerDialog {

    /** list action id constant. */
    public static final String LIST_DETAIL_RELATIONS = "dr";

    /** list action id constant. */
    public static final String LIST_DETAIL_RELATIONS_HIDE = "drh";

    /** list action id constant. */
    public static final String LIST_DETAIL_RELATIONS_SHOW = "drs";

    /** list id constant. */
    public static final String LIST_ID = "dbr";

    /** The internal collector instance. */
    private I_CmsListResourceCollector m_collector;

    /** The broken relations validator object. */
    private CmsRelationDeleteValidator m_validator;

    /**
     * Public constructor.<p>
     * 
     * @param jsp an initialized JSP action element
     * @param resources the list of resources to check
     * @param includeSiblings if siblings should included
     */
    public CmsDeleteBrokenRelationsList(CmsJspActionElement jsp, List resources, boolean includeSiblings) {

        super(jsp, LIST_ID, Messages.get().container(Messages.GUI_DELETE_BROKENRELATIONS_LIST_NAME_0));
        String relativeTo = CmsResource.getParentFolder((String)resources.get(0));

        m_validator = new CmsRelationDeleteValidator(getCms(), resources, includeSiblings);
        List resourceList = new ArrayList(m_validator.keySet());
        Collections.sort(resourceList);

        m_collector = new CmsDeleteBrokenRelationsCollector(this, resourceList);

        // prevent paging
        getList().setMaxItemsPerPage(Integer.MAX_VALUE);

        // set the right resource util parameters
        CmsResourceUtil resUtil = getResourceUtil();
        resUtil.setAbbrevLength(50);
        resUtil.setRelativeTo(relativeTo);
        resUtil.setSiteMode(CmsResourceUtil.SITE_MODE_MATCHING);
    }

    /**
     * @see org.opencms.workplace.list.A_CmsListDialog#executeListMultiActions()
     */
    public void executeListMultiActions() {

        throwListUnsupportedActionException();
    }

    /**
     * @see org.opencms.workplace.list.A_CmsListDialog#executeListSingleActions()
     */
    public void executeListSingleActions() {

        throwListUnsupportedActionException();
    }

    /**
     * @see org.opencms.workplace.list.A_CmsListExplorerDialog#getCollector()
     */
    public I_CmsListResourceCollector getCollector() {

        return m_collector;
    }

    /**
     * Returns the validator.<p>
     *
     * @return the validator
     */
    public CmsRelationDeleteValidator getValidator() {

        return m_validator;
    }

    /**
     * @see org.opencms.workplace.list.A_CmsListDialog#fillDetails(java.lang.String)
     */
    protected void fillDetails(String detailId) {

        // get content
        List resourceNames = getList().getAllContent();
        Iterator itResourceNames = resourceNames.iterator();
        while (itResourceNames.hasNext()) {
            CmsListItem item = (CmsListItem)itResourceNames.next();
            String resourceName = getResourceUtil(item).getResource().getRootPath();

            StringBuffer html = new StringBuffer(128);
            if (detailId.equals(LIST_DETAIL_RELATIONS)) {
                // relations
                CmsRelationValidatorInfoEntry infoEntry = m_validator.getInfoEntry(resourceName);
                Iterator itRelations = infoEntry.getRelations().iterator();

                // show all links that will get broken
                while (itRelations.hasNext()) {
                    CmsRelation relation = (CmsRelation)itRelations.next();
                    String relationName = relation.getSourcePath();
                    if (relationName.startsWith(infoEntry.getSiteRoot())) {
                        // same site
                        relationName = relationName.substring(infoEntry.getSiteRoot().length());
                    } else {
                        // other site
                        String site = CmsSiteManager.getSiteRoot(relationName);
                        String siteName = site;
                        if (site != null) {
                            relationName = relationName.substring(site.length());
                            siteName = CmsSiteManager.getSite(site).getTitle();
                        } else {
                            siteName = "/";
                        }
                        relationName = key(Messages.GUI_DELETE_SITE_RELATION_2, new Object[] {siteName, relationName});
                    }
                    relationName = CmsStringUtil.formatResourceName(relationName, 50);
                    html.append(relationName);
                    html.append("&nbsp;<span style='color: #666666;'>(");
                    html.append(relation.getType().getLocalizedName(getLocale()));
                    html.append(")</span>");
                    if (itRelations.hasNext()) {
                        html.append("<br>");
                    }
                    html.append("\n");
                }
            } else {
                continue;
            }
            item.set(detailId, html.toString());
        }
    }

    /**
     * @see org.opencms.workplace.list.A_CmsListExplorerDialog#getListItems()
     */
    protected List getListItems() throws CmsException {

        String storedSiteRoot = getCms().getRequestContext().getSiteRoot();
        try {
            getCms().getRequestContext().setSiteRoot("");
            return super.getListItems();
        } finally {
            getCms().getRequestContext().setSiteRoot(storedSiteRoot);
        }
    }

    /**
     * @see org.opencms.workplace.list.A_CmsListExplorerDialog#isColumnVisible(int)
     */
    protected boolean isColumnVisible(int colFlag) {

        boolean isVisible = (colFlag == CmsUserSettings.FILELIST_TITLE);
        isVisible = isVisible || (colFlag == LIST_COLUMN_TYPEICON.hashCode());
        isVisible = isVisible || (colFlag == LIST_COLUMN_LOCKICON.hashCode());
        isVisible = isVisible || (colFlag == LIST_COLUMN_PROJSTATEICON.hashCode());
        isVisible = isVisible || (colFlag == LIST_COLUMN_NAME.hashCode());
        isVisible = isVisible || (colFlag == LIST_COLUMN_SITE.hashCode());
        return isVisible;
    }

    /**
     * @see org.opencms.workplace.list.A_CmsListDialog#setColumns(org.opencms.workplace.list.CmsListMetadata)
     */
    protected void setColumns(CmsListMetadata metadata) {

        super.setColumns(metadata);

        Iterator it = metadata.getColumnDefinitions().iterator();
        while (it.hasNext()) {
            CmsListColumnDefinition colDefinition = (CmsListColumnDefinition)it.next();
            colDefinition.setSorteable(false);
            if (colDefinition.getId().equals(LIST_COLUMN_NAME)) {
                colDefinition.removeDefaultAction(LIST_DEFACTION_OPEN);
                colDefinition.setWidth("60%");
            }
        }
    }

    /**
     * @see org.opencms.workplace.list.A_CmsListDialog#setIndependentActions(org.opencms.workplace.list.CmsListMetadata)
     */
    protected void setIndependentActions(CmsListMetadata metadata) {

        /**
         * Class to render a javascript driven detail action button.<p>
         */
        abstract class DetailsJsAction extends A_CmsListIndependentJsAction {

            /**
             * Default constructor.<p>
             * 
             * @param id the action id
             */
            public DetailsJsAction(String id) {

                super(id);
            }

            /**
             * @see org.opencms.workplace.list.CmsListIndependentAction#buttonHtml(org.opencms.workplace.CmsWorkplace)
             */
            public String buttonHtml(CmsWorkplace wp) {

                StringBuffer html = new StringBuffer(1024);
                html.append("\t<span id='");
                html.append(getId());
                html.append("' class=\"link");
                html.append("\"");
                html.append(" onClick=\"");
                html.append(resolveOnClic(wp));
                html.append("\"");
                html.append(">");
                html.append("<img src='");
                html.append(CmsWorkplace.getSkinUri());
                html.append(getIconPath());
                html.append("'");
                html.append(" alt='");
                html.append(getName().key(wp.getLocale()));
                html.append("'");
                html.append(" title='");
                html.append(getName().key(wp.getLocale()));
                html.append("'");
                html.append(">");
                html.append("&nbsp;");
                html.append("<a href='#'>");
                html.append(getName().key(wp.getLocale()));
                html.append("</a>");
                html.append("</span>");
                return html.toString();
            }
        }

        I_CmsListAction hideAction = new DetailsJsAction(LIST_DETAIL_RELATIONS_HIDE) {

            /**
             * @see org.opencms.workplace.list.A_CmsListIndependentJsAction#jsCode(CmsWorkplace)
             */
            public String jsCode(CmsWorkplace wp) {

                return "javascript:showBrokenLinks(false);";
            }
        };
        hideAction.setIconPath(A_CmsListDialog.ICON_DETAILS_HIDE);
        hideAction.setName(Messages.get().container(Messages.GUI_DELETE_BROKENRELATIONS_DETAIL_HIDE_RELATIONS_NAME_0));
        hideAction.setHelpText(Messages.get().container(
            Messages.GUI_DELETE_BROKENRELATIONS_DETAIL_HIDE_RELATIONS_HELP_0));
        metadata.addIndependentAction(hideAction);

        I_CmsListAction showAction = new DetailsJsAction(LIST_DETAIL_RELATIONS_SHOW) {

            /**
             * @see org.opencms.workplace.list.A_CmsListIndependentJsAction#jsCode(CmsWorkplace)
             */
            public String jsCode(CmsWorkplace wp) {

                return "javascript:showBrokenLinks(true);";
            }
        };
        showAction.setIconPath(A_CmsListDialog.ICON_DETAILS_SHOW);
        showAction.setName(Messages.get().container(Messages.GUI_DELETE_BROKENRELATIONS_DETAIL_SHOW_RELATIONS_NAME_0));
        showAction.setHelpText(Messages.get().container(
            Messages.GUI_DELETE_BROKENRELATIONS_DETAIL_SHOW_RELATIONS_HELP_0));
        metadata.addIndependentAction(showAction);

        // create list item detail
        CmsListItemDetails relationsDetails = new CmsListItemDetails(LIST_DETAIL_RELATIONS) {

            /**
             * @see org.opencms.workplace.list.CmsListItemDetails#getAction()
             */
            public I_CmsListAction getAction() {

                return new CmsListIndependentAction("hide") {

                    /**
                     * @see org.opencms.workplace.list.CmsListIndependentAction#buttonHtml(org.opencms.workplace.CmsWorkplace)
                     */
                    public String buttonHtml(CmsWorkplace wp) {

                        return "";
                    }
                };
            }
        };
        relationsDetails.setAtColumn(LIST_COLUMN_ROOT_PATH);
        relationsDetails.setVisible(true);
        relationsDetails.setFormatter(new CmsListItemDetailsFormatter(Messages.get().container(
            Messages.GUI_DELETE_BROKENRELATIONS_LABEL_RELATIONS_0)));
        relationsDetails.setShowActionName(Messages.get().container(
            Messages.GUI_DELETE_BROKENRELATIONS_DETAIL_SHOW_RELATIONS_NAME_0));
        relationsDetails.setShowActionHelpText(Messages.get().container(
            Messages.GUI_DELETE_BROKENRELATIONS_DETAIL_SHOW_RELATIONS_HELP_0));
        relationsDetails.setHideActionName(Messages.get().container(
            Messages.GUI_DELETE_BROKENRELATIONS_DETAIL_HIDE_RELATIONS_NAME_0));
        relationsDetails.setHideActionHelpText(Messages.get().container(
            Messages.GUI_DELETE_BROKENRELATIONS_DETAIL_HIDE_RELATIONS_HELP_0));

        // add resources info item detail to meta data
        metadata.addItemDetails(relationsDetails);
    }

    /**
     * @see org.opencms.workplace.list.A_CmsListDialog#setMultiActions(org.opencms.workplace.list.CmsListMetadata)
     */
    protected void setMultiActions(CmsListMetadata metadata) {

        // no LMAs, and remove default search action
        metadata.setSearchAction(null);
    }
}