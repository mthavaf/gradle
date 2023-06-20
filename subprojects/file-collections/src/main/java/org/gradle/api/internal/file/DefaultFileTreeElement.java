/*
 * Copyright 2009 the original author or authors.
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
package org.gradle.api.internal.file;

import org.apache.commons.io.IOUtils;
import org.gradle.api.GradleException;
import org.gradle.api.UncheckedIOException;
import org.gradle.api.file.FilePermissions;
import org.gradle.api.file.RelativePath;
import org.gradle.internal.file.Chmod;
import org.gradle.internal.file.Stat;
import org.gradle.internal.nativeintegration.filesystem.FileSystem;
import org.gradle.util.internal.GFileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public class DefaultFileTreeElement extends AbstractFileTreeElement {
    private final File file;
    private final RelativePath relativePath;
    private final Stat stat;

    public DefaultFileTreeElement(File file, RelativePath relativePath, Chmod chmod, Stat stat) {
        super(chmod);
        this.file = file;
        this.relativePath = relativePath;
        this.stat = stat;
    }

    public static DefaultFileTreeElement of(File file, FileSystem fileSystem) {
        RelativePath path = RelativePath.parse(!file.isDirectory(), file.getAbsolutePath());
        return new DefaultFileTreeElement(file, path, fileSystem, fileSystem);
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public String getDisplayName() {
        return "file '" + file + "'";
    }

    @Override
    public long getLastModified() {
        return file.lastModified();
    }

    @Override
    public long getSize() {
        return file.length();
    }

    @Override
    public boolean isDirectory() {
        return !relativePath.isFile();
    }

    @Override
    public boolean isSymbolicLink() {
        return Files.isSymbolicLink(file.toPath());
    }

    @Override
    public String getSymbolicLinkTarget() {
        try {
            return Files.readSymbolicLink(file.toPath()).toString();
        } catch (IOException e) {
            throw new GradleException(String.format("Couldn't read symbolic link '%s'.", file.toPath()), e);
        }
    }

    @Override
    public void copyTo(OutputStream output) {
        try (InputStream inputStream = GFileUtils.openInputStream(file)) {
            IOUtils.copyLarge(inputStream, output);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public RelativePath getRelativePath() {
        return relativePath;
    }

    @Override
    public FilePermissions getImmutablePermissions() {
        int unixNumeric = stat.getUnixMode(file);
        return new DefaultFilePermissions(unixNumeric);
    }
}
