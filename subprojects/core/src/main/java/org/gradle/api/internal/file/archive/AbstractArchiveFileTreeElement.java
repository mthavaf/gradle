/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.file.archive;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.io.IOUtils;
import org.gradle.api.GradleException;
import org.gradle.api.UncheckedIOException;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.RelativePath;
import org.gradle.api.internal.file.AbstractFileTreeElement;
import org.gradle.internal.file.Chmod;
import org.gradle.util.internal.GFileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An implementation of {@link org.gradle.api.file.FileTreeElement FileTreeElement} meant
 * for use with archive files when subclassing {@link org.gradle.api.internal.file.AbstractFileTree AbstractFileTree}.
 * <p>
 * This implementation extracts the files from the archive to the supplied expansion directory.
 */
public abstract class AbstractArchiveFileTreeElement extends AbstractFileTreeElement implements FileVisitDetails {
    private final File expandedDir;
    private File file;
    private final AtomicBoolean stopFlag;

    /**
     * Creates a new instance.
     *
     * @param chmod the chmod instance to use
     * @param expandedDir the directory to extract the archived file to
     * @param stopFlag the stop flag to use
     */
    protected AbstractArchiveFileTreeElement(Chmod chmod, File expandedDir, AtomicBoolean stopFlag) {
        super(chmod);
        this.expandedDir = expandedDir;
        this.stopFlag = stopFlag;
    }

    /**
     * Returns the archive entry for this element.
     *
     * @return the archive entry
     * @implSpec this method should be overriden to return a more specific type
     */
    protected abstract ArchiveEntry getArchiveEntry();

    /**
     * Returns a safe name for the name of a file contained in the archive.
     * @see org.gradle.util.internal.ZipSlip#safeZipEntryName(String)
     */
    protected abstract String safeEntryName();

    @Override
    public File getFile() {
        if (file == null) {
            file = new File(expandedDir, safeEntryName());
            if (!file.exists()) {
                GFileUtils.mkdirs(file.getParentFile());
                if (isSymbolicLink()) {
                    copySymlinkTo(file);
                } else {
                    copyFileTo(file);
                }
            }
        }
        return file;
    }

    /**
     * Opens this file as an input stream. Generally, calling this method is more performant than calling {@code new
     * FileInputStream(getFile())}.
     *
     * @return The input stream. Never returns null. The caller is responsible for closing this stream.
     */
    @SuppressWarnings("deprecation") // TODO: remove deprecation suppression after FileTreeElement.open() is removed
    abstract public InputStream open();

    @Override
    public void copyTo(OutputStream output) {
        try {
            try (InputStream inputStream = open()) {
                IOUtils.copyLarge(inputStream, output);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @SuppressWarnings("deprecation")
    protected void copyFileTo(File target) { //FIXME: permissions?
        try {
            if (isDirectory()) {
                GFileUtils.mkdirs(target);
            } else {
                GFileUtils.mkdirs(target.getParentFile());
                try (FileOutputStream outputStream = new FileOutputStream(target)) {
                    copyTo(outputStream);
                }
            }
        } catch (Exception e) {
            throw new GradleException(String.format("Could not extract %s to '%s'.", getDisplayName(), target), e);
        }
    }

    protected void copySymlinkTo(File target) { //FIXME: permissions?
        Path targetPath = new File(getSymbolicLinkTarget().replace("/", File.separator)).toPath();
        try {
            Files.createSymbolicLink(target.toPath(), targetPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public RelativePath getRelativePath() {
        return new RelativePath(!getArchiveEntry().isDirectory(), safeEntryName().split("/"));
    }

    @Override
    public long getLastModified() {
        return getArchiveEntry().getLastModifiedDate().getTime();
    }

    @Override
    public boolean isDirectory() {
        return getArchiveEntry().isDirectory();
    }

    @Override
    public long getSize() {
        return getArchiveEntry().getSize();
    }

    @Override
    public void stopVisiting() {
        stopFlag.set(true);
    }
}
