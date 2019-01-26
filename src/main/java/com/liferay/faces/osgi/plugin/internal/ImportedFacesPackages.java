/**
 * Copyright (c) 2000-2019 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package com.liferay.faces.osgi.plugin.internal;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;


/**
 * @author  Kyle Stiemann
 */
public final class ImportedFacesPackages {

	// Private Final Data Members
	private final Set<String> importedPackages;

	public ImportedFacesPackages() {

		importedPackages = new LinkedHashSet<String>();
		importPackage(ImportedFacesPackages.class);
	}

	public Set<String> getImportedPackages() {
		return Collections.unmodifiableSet(importedPackages);
	}

	private void importPackage(Class<?> clazz) {
		importedPackages.add(clazz.getPackage().getName());
	}
}
