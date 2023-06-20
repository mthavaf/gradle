/*
 * Copyright 2013 the original author or authors.
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

package org.gradle.api.internal.file.copy;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Transformer;
import org.gradle.api.file.ConfigurableFilePermissions;
import org.gradle.api.file.ContentFilterable;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.file.ExpandDetails;
import org.gradle.api.file.FilePermissions;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.LinksStrategy;
import org.gradle.api.file.RelativePath;
import org.gradle.api.internal.file.DefaultConfigurableFilePermissions;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.internal.Actions;
import org.gradle.internal.exceptions.Contextual;
import org.gradle.internal.file.Chmod;
import org.gradle.util.internal.GFileUtils;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class DefaultFileCopyDetails implements FileVisitDetails, FileCopyDetailsInternal {
    private final FileVisitDetails fileDetails;
    private final CopySpecResolver specResolver;
    private final FilterChain filterChain;
    private final ObjectFactory objectFactory;
    private boolean defaultDuplicatesStrategy;
    private RelativePath relativePath;
    private boolean excluded;

    private DefaultConfigurableFilePermissions permissions;
    private DuplicatesStrategy duplicatesStrategy;
    private LinksStrategy preserveLinks;
    private Chmod chmod;

    @Inject
    public DefaultFileCopyDetails(FileVisitDetails fileDetails, CopySpecResolver specResolver, ObjectFactory objectFactory, Chmod chmod) {
        this.filterChain = new FilterChain(specResolver.getFilteringCharset());
        this.fileDetails = fileDetails;
        this.specResolver = specResolver;
        this.objectFactory = objectFactory;
        this.duplicatesStrategy = specResolver.getDuplicatesStrategy();
        this.defaultDuplicatesStrategy = specResolver.isDefaultDuplicateStrategy();
        this.preserveLinks = specResolver.getPreserveLinks();
        this.chmod = chmod;
    }

    @Override
    public boolean isIncludeEmptyDirs() {
        return specResolver.getIncludeEmptyDirs();
    }

    @Override
    public void stopVisiting() {
        fileDetails.stopVisiting();
    }

    @Override
    public File getFile() {
        if (filterChain.hasFilters()) {
            throw new UnsupportedOperationException();
        } else {
            return fileDetails.getFile();
        }
    }

    @Override
    public void copyTo(OutputStream output) {
        fileDetails.copyTo(output);
    }

    @Override
    public boolean isDirectory() {
        return fileDetails.isDirectory();
    }

    @Override
    public boolean isSymbolicLink() {
        return fileDetails.isSymbolicLink();
    }

    @Override
    public String getSymbolicLinkTarget() {
        return fileDetails.getSymbolicLinkTarget();
    }

    @Override
    public long getLastModified() {
        return fileDetails.getLastModified();
    }

    @Override
    public long getSize() {
        if (filterChain.hasFilters()) {
            ByteCountingOutputStream outputStream = new ByteCountingOutputStream();
            copyTo(outputStream);
            return outputStream.size;
        } else {
            return fileDetails.getSize();
        }
    }

    @Override
    public String getName() {
        return fileDetails.getName();
    }

    @Override
    public String getPath() {
        return fileDetails.getPath();
    }

    @Override
    @SuppressWarnings("deprecation") //TODO: remove suppression after method is removed from the interface
    public boolean copyTo(File target) {
        validateTimeStamps();
        try {
            if (isSymbolicLink()) {
                copySymlinkTo(target); //TODO: permissions?
            } else if (isDirectory()) {
                GFileUtils.mkdirs(target);
                adaptPermissions(target);
            } else {
                GFileUtils.mkdirs(target.getParentFile());
                copyFile(target);
                adaptPermissions(target);
            }
            return true;
        } catch (Exception e) {
            throw new CopyFileElementException(String.format("Could not copy %s to '%s'.", getName(), target), e);
        }
    }

    @SuppressWarnings("deprecation")
    private void copyFile(File target) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(target)) {
            copyTo(outputStream);
        }
    }

    private void copySymlinkTo(File target) {
        try {
            Files.copy(getFile().toPath(), target.toPath(), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void adaptPermissions(File target) {
        int specMode = getImmutablePermissions().toUnixNumeric();
        chmod.chmod(target, specMode);
    }

    @Override
    public RelativePath getRelativePath() {
        if (relativePath == null) {
            RelativePath path = fileDetails.getRelativePath();
            relativePath = specResolver.getDestPath().append(path.isFile(), path.getSegments());
        }
        return relativePath;
    }

    @Override
    public int getMode() {
        return 0;
    }

    @Override
    public FilePermissions getImmutablePermissions() {
        if (permissions != null) {
            return permissions;
        }

        Provider<FilePermissions> specMode = getSpecMode();
        if (specMode.isPresent()) {
            return specMode.get();
        }

        return fileDetails.getImmutablePermissions();
    }

    private Provider<FilePermissions> getSpecMode() {
        return fileDetails.isDirectory() ? specResolver.getImmutableDirPermissions() : specResolver.getImmutableFilePermissions();
    }

    @Override
    public void setRelativePath(RelativePath path) {
        this.relativePath = path;
    }

    @Override
    public void setName(String name) {
        relativePath = getRelativePath().replaceLastName(name);
    }

    @Override
    public void setPath(String path) {
        relativePath = RelativePath.parse(getRelativePath().isFile(), path);
    }

    boolean isExcluded() {
        return excluded;
    }

    @Override
    public void exclude() {
        excluded = true;
    }

    @Override
    public void setMode(int mode) {
        getPermissions().unix(mode);
    }

    @Override
    public void permissions(Action<? super ConfigurableFilePermissions> configureAction) {
        configureAction.execute(getPermissions());
    }

    @Override
    public void setPermissions(FilePermissions permissions) {
        getPermissions().unix(permissions.toUnixNumeric());
    }

    private DefaultConfigurableFilePermissions getPermissions() {
        if (permissions == null) {
            permissions = objectFactory.newInstance(DefaultConfigurableFilePermissions.class, objectFactory, DefaultConfigurableFilePermissions.getDefaultUnixNumeric(fileDetails.isDirectory()));
        }
        return permissions;
    }

    @Override
    public ContentFilterable filter(Closure closure) {
        return filter(new ClosureBackedTransformer(closure));
    }

    @Override
    public ContentFilterable filter(Transformer<String, String> transformer) {
        filterChain.add(transformer);
        return this;
    }

    @Override
    public ContentFilterable filter(Map<String, ?> properties, Class<? extends FilterReader> filterType) {
        filterChain.add(filterType, properties);
        return this;
    }

    @Override
    public ContentFilterable filter(Class<? extends FilterReader> filterType) {
        filterChain.add(filterType);
        return this;
    }

    @Override
    public ContentFilterable expand(Map<String, ?> properties) {
        return expand(properties, Actions.doNothing());
    }

    @Override
    public ContentFilterable expand(Map<String, ?> properties, Action<? super ExpandDetails> action) {
        ExpandDetails details = objectFactory.newInstance(ExpandDetails.class);
        details.getEscapeBackslash().convention(false);
        action.execute(details);
        filterChain.expand(properties, details.getEscapeBackslash());
        return this;
    }

    @Override
    public void setDuplicatesStrategy(DuplicatesStrategy strategy) {
        this.duplicatesStrategy = strategy;
        this.defaultDuplicatesStrategy = strategy == DuplicatesStrategy.INHERIT;
    }

    @Override
    public DuplicatesStrategy getDuplicatesStrategy() {
        return this.duplicatesStrategy;
    }

    public boolean isDefaultDuplicatesStrategy() {
        return defaultDuplicatesStrategy;
    }

    @Override
    public String getSourceName() {
        return this.fileDetails.getName();
    }

    @Override
    public String getSourcePath() {
        return this.fileDetails.getPath();
    }

    @Override
    public RelativePath getRelativeSourcePath() {
        return this.fileDetails.getRelativePath();
    }

    @Override
    public LinksStrategy getPreserveLinks() { //TODO: save the result of preserving strategy check
        return preserveLinks;
    }

    private static class ByteCountingOutputStream extends OutputStream {
        long size;

        @Override
        public void write(int b) throws IOException {
            size++;
        }

        @Override
        public void write(byte[] b) throws IOException {
            size += b.length;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            size += len;
        }
    }

    protected void validateTimeStamps() {
        final long lastModified = getLastModified();
        if (lastModified < 0) {
            throw new GradleException(String.format("Invalid Timestamp %s for '%s'.", lastModified, getName()));
        }
    }

    @Contextual
    private static class CopyFileElementException extends GradleException {
        CopyFileElementException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
