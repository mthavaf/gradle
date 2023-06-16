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

import org.gradle.api.file.FilePermissions;
import org.gradle.api.file.ReadOnlyFileTreeElement;

public abstract class AbstractReadOnlyFileTreeElement implements ReadOnlyFileTreeElement {
    public abstract String getDisplayName();

    @Override
    public String toString() {
        return getDisplayName();
    }

    @Override
    public String getName() {
        return getRelativePath().getLastName();
    }

    @Override
    public String getPath() {
        return getRelativePath().getPathString();
    }

    @Override
    public int getMode() {
        return getImmutablePermissions().toUnixNumeric();
    }

    @Override
    public FilePermissions getImmutablePermissions() {
        return isDirectory() ? DefaultFilePermissions.DEFAULT_DIR_PERMISSIONS :
            DefaultFilePermissions.DEFAULT_FILE_PERMISSIONS;
    }
}
