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
import java.util.Set;
import org.objectweb.asm.ClassReader;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;


/**
 * @author  Kyle Stiemann
 */
/* package-private */ final class ImportedFacesPackagesClassByteCodeUtil {

	// Package-Private Constants
	/* package-private */ static final String IMPORTED_FACES_PACKAGES_CLASS_FILE =
		ImportedFacesPackages.class.getSimpleName() + ".class";

	private ImportedFacesPackagesClassByteCodeUtil() {
		throw new AssertionError();
	}

	/* package-private */ static byte[] getByteCode(Set<String> additionalPackagesToImport) throws IOException {
		
		byte[] importedFacesPackagesClassBytes = getClassBytes();
		ClassReader classReader = new ClassReader(importedFacesPackagesClassBytes);
		ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		ClassVisitor importFacesPackageClassVisitorImpl = new ImportFacesPackagesClassVisitorImpl(classWriter,
			additionalPackagesToImport);
		classReader.accept(importFacesPackageClassVisitorImpl, ClassReader.SKIP_FRAMES);

		return classWriter.toByteArray();
	}

	private static byte[] getClassBytes() throws IOException {

		InputStream inputStream = null;

		try {

			inputStream = ImportedFacesPackages.class
				.getResourceAsStream(IMPORTED_FACES_PACKAGES_CLASS_FILE);
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

	private static final class ImportFacesPackagesClassVisitorImpl extends ClassVisitor {

		// Private Final Data Members
		private final Set<String> importedPackageClassNames;

		private ImportFacesPackagesClassVisitorImpl(ClassWriter classWriter, Set<String> importedPackageClassNames) {

			super(Opcodes.ASM5, classWriter);

			this.importedPackageClassNames = importedPackageClassNames;
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

			MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);

			if ("<init>".equals(name)) {
				methodVisitor = new ImportFacesPackageMethodVisitorImpl(methodVisitor, access, name, desc,
					importedPackageClassNames);
			}

			return methodVisitor;
		}
	}

	private static final class ImportFacesPackageMethodVisitorImpl extends GeneratorAdapter {

		// Private Final Data Members
		private final Set<String> importedPackageClassNames;

		private ImportFacesPackageMethodVisitorImpl(MethodVisitor methodVisitor, int access, String name,
			String descriptor, Set<String> importedPackageClassNames) {

			super(Opcodes.ASM5, methodVisitor, access, name, descriptor);

			this.importedPackageClassNames = importedPackageClassNames;
		}

		private static String getTypeString(String className) {
			return className.replace(".", "/");
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {

			super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);

			if ("importPackage".equals(name)) {

				for (String importedPackageClassName : importedPackageClassNames) {

					super.loadThis();
					super.visitLdcInsn(Type.getObjectType(getTypeString(importedPackageClassName)));
					super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
				}
			}
		}
	}
}
