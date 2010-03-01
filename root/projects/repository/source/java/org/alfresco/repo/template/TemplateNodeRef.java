/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.repo.template;

import java.io.Serializable;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

/**
 * Contract supported by Template API objects that represent a repository object via a NodeRef
 * and associated minimum properties such as as Type and Name.
 * 
 * @author Kevin Roast
 */
public interface TemplateNodeRef extends Serializable
{
    /**
     * @return The GUID for the node
     */
    public String getId();
    
    /**
     * @return Returns the NodeRef this Node object represents
     */
    public NodeRef getNodeRef();
    
    /**
     * @return Returns the type.
     */
    public QName getType();
    
    /**
     * @return The display name for the node
     */
    public String getName();
}
