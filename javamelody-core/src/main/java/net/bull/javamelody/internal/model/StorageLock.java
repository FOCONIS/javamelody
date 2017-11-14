/*
 * Copyright 2008-2017 by Emeric Vernat
 *
 *     This file is part of Java Melody.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bull.javamelody.internal.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

import net.bull.javamelody.internal.common.Parameters;

/**
 * Lock sur le répertoire de stockage pour détecter si des instances distinctes écrivent dans le même répertoire.
 * @author Emeric Vernat
 */
class StorageLock {
	private static final String LOCK_FILENAME = "javamelody.lock";
	private final RandomAccessFile input;
	private final FileChannel fileChannel;
	private FileLock fileLock;

	StorageLock(String application) {
		super();
		final File storageDir = Parameters.getStorageDirectory(application);
		if (!storageDir.mkdirs() && !storageDir.exists()) {
			throw new IllegalStateException(
					"JavaMelody directory can't be created: " + storageDir.getPath());
		}
		final File lockFile = new File(storageDir, LOCK_FILENAME);
		try {
			this.input = new RandomAccessFile(lockFile, "rw");
			this.fileChannel = input.getChannel();
		} catch (final FileNotFoundException e) {
			throw new IllegalArgumentException(e);
		}
		// initialize the lock
		getFileLock();
	}

	void release() throws IOException {
		try {
			if (fileLock != null) {
				fileLock.release();
			}
		} finally {
			try {
				fileChannel.close();
			} finally {
				input.close();
			}
		}
	}

	private FileLock getFileLock() {
		if (fileLock == null) {
			try {
				fileLock = fileChannel.tryLock();
			} catch (final IOException e) {
				return null;
			} catch (final OverlappingFileLockException e) {
				return null;
			}
		}
		return fileLock;
	}

	boolean isAcquired() {
		return getFileLock() != null;
	}
}
