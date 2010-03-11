/*
 * File   : $Source: /alkacon/cvs/opencms/src-modules/org/opencms/ade/shared/rpc/Attic/I_CmsSitemapService.java,v $
 * Date   : $Date: 2010/03/11 11:26:13 $
 * Version: $Revision: 1.2 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (C) 2002 - 2009 Alkacon Software (http://www.alkacon.com)
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

package org.opencms.ade.shared.rpc;

import org.opencms.ade.shared.CmsClientSitemapEntry;
import org.opencms.gwt.shared.rpc.CmsRpcException;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * Handles all RPC services related to the sitemap.<p>
 * 
 * @author Michael Moossen
 * 
 * @version $Revision: 1.2 $ 
 * 
 * @since 8.0.0
 * 
 * @see org.opencms.ade.CmsSitemapService
 * @see org.opencms.ade.shared.rpc.I_CmsSitemapService
 * @see org.opencms.ade.shared.rpc.I_CmsSitemapServiceAsync
 */
@RemoteServiceRelativePath("org.opencms.ade.CmsSitemapService.gwt")
public interface I_CmsSitemapService extends RemoteService {

    /**
     * Returns the sitemap entry for the given path.<p>
     * 
     * @param root the site relative root
     *  
     * @return the sitemap entry
     * 
     * @throws CmsRpcException if something goes wrong 
     */
    CmsClientSitemapEntry getSitemapEntry(String root) throws CmsRpcException;

    /**
     * Returns the sitemap children for the given path.<p>
     * 
     * @param root the site relative root
     *  
     * @return the sitemap children
     * 
     * @throws CmsRpcException if something goes wrong 
     */
    CmsClientSitemapEntry[] getSitemapChildren(String root) throws CmsRpcException;
}