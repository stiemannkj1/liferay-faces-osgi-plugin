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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;


/**
 * @author  Kyle Stiemann
 */
/* package-private */ final class ServletContainerInitializerUtil {

	private ServletContainerInitializerUtil() {
		throw new AssertionError();
	}

	/* package-private */ static Set<String> getDefaultClassNames() throws IOException {

		Set<String> servletConainerIntializerClasses = new LinkedHashSet<String>();
		InputStream inputStream = null;
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;

		try {

			inputStream = LiferayFacesOSGiPluginMojo.class.getResourceAsStream("/" +
				LiferayFacesOSGiPluginMojo.SERVLET_CONTAINER_INITIALIZER_FILE_PATH);
			inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
			bufferedReader = new BufferedReader(inputStreamReader);

			bufferedReader.lines().filter((String line) -> { return !line.trim().isEmpty(); }).forEach((line) -> {
				servletConainerIntializerClasses.add(line);
			});
		}
		catch (UncheckedIOException e) {
			throw new IOException(e);
		}
		finally {

			CloseableUtil.close(bufferedReader);
			CloseableUtil.close(inputStreamReader);
			CloseableUtil.close(inputStream);
		}

		return Collections.unmodifiableSet(servletConainerIntializerClasses);
	}
}
