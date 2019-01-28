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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * @author  Kyle Stiemann
 */
/* package-private */ final class ClassFileUtil {

	private ClassFileUtil() {
		throw new AssertionError();
	}

	/* package-private */ static byte[] getClassBytes(Class<?> clazz) throws IOException {

		InputStream inputStream = null;

		try {

			String classFileName = getClassFileName(clazz);
			inputStream = clazz.getResourceAsStream(classFileName);

			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

			byte[] data = new byte[(1024 * 16)];
			int lengthRead = inputStream.read(data, 0, data.length);

			while (lengthRead > -1) {

				byteArrayOutputStream.write(data, 0, lengthRead);
				lengthRead = inputStream.read(data, 0, data.length);
			}

			return byteArrayOutputStream.toByteArray();
		}
		finally {
			CloseableUtil.close(inputStream);
		}
	}

	/* package-private */ static String getClassFileName(Class<?> clazz) {
		return clazz.getSimpleName() + ".class";
	}
}
