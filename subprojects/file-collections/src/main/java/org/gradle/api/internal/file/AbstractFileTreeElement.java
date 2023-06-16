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

import org.gradle.api.GradleException;
import org.gradle.api.file.FileTreeElement;
import org.gradle.internal.exceptions.Contextual;
import org.gradle.internal.file.Chmod;
import org.gradle.util.internal.GFileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public abstract class AbstractFileTreeElement extends AbstractReadOnlyFileTreeElement implements FileTreeElement {
    private final Chmod chmod;

    protected AbstractFileTreeElement(Chmod chmod) {
        this.chmod = chmod;
    }

    @Override
    @Deprecated
    // TODO: remove after FileTreeElement.copyTo(File) is removed
    public boolean copyTo(File target) {
        logDeprecation();
        validateTimeStamps();
        try {
            if (isDirectory()) {
                GFileUtils.mkdirs(target);
            } else {
                GFileUtils.mkdirs(target.getParentFile());
                copyFile(target);
            }
            chmod.chmod(target, getImmutablePermissions().toUnixNumeric());
            return true;
        } catch (Exception e) {
            throw new CopyFileElementException(String.format("Could not copy %s to '%s'.", getDisplayName(), target), e);
        }
    }

    @Deprecated
    private void copyFile(File target) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(target)) {
            copyTo(outputStream);
        }
    }

    protected void validateTimeStamps() {
        final long lastModified = getLastModified();
        if (lastModified < 0) {
            throw new GradleException(String.format("Invalid Timestamp %s for '%s'.", lastModified, getDisplayName()));
        }
    }

    @Contextual
    @Deprecated
    private static class CopyFileElementException extends GradleException {
        CopyFileElementException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static void logDeprecation() {
        throw new UnsupportedOperationException("Should not be called");
        //TODO: uncomment after all tests are passed
//        DeprecationLogger.deprecateMethod(FileTreeElement.class, "copyTo(File)")
//            .willBeRemovedInGradle9()
//            .withUpgradeGuideSection(8, "filetree_deprecations") //FIXME
//            .nagUser();
    }
}
