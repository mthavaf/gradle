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
package org.gradle.api.file;

import java.io.File;
import java.io.OutputStream;

/**
 * Provides access to details about a file or directory being visited by a {@link FileVisitor}.
 *
 * @see FileTree#visit(groovy.lang.Closure)
 */
public interface FileVisitDetails extends ReadOnlyFileTreeElement {
    /**
     * Returns the file being visited.
     * Note that this may be a copy of the original file (e.g. when visiting an archive).
     *
     * @return The file. Never returns null.
     */
    File getFile();

    /**
     * Copies the content of this file to an output stream. Generally, calling this method is more performant than
     * calling {@code new FileInputStream(getFile())}.
     *
     * @param output The output stream to write to. The caller is responsible for closing this stream.
     */
    void copyTo(OutputStream output);

    /**
     * Requests that file visiting terminate after the current file.
     */
    void stopVisiting();
}
