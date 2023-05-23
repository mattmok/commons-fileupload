/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.fileupload2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.fileupload2.pub.FileUploadByteCountLimitException;
import org.apache.commons.fileupload2.pub.FileUploadSizeException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

/**
 * Unit test for items with varying sizes.
 *
 * @param <F> The subclass of FileUpload.
 * @param <R> The type of FileUpload request.
 */
public abstract class AbstractSizesTest<F extends FileUpload<R>, R> extends AbstractTest<F, R> {

    /**
     * Checks, whether limiting the file size works.
     */
    @Test
    public void testFileSizeLimit() throws IOException, FileUploadException {
        // @formatter:off
        final String request =
            "-----1234\r\n" +
            "Content-Disposition: form-data; name=\"file\"; filename=\"foo.tab\"\r\n" +
            "Content-Type: text/whatever\r\n" +
            "\r\n" +
            "This is the content of the file\n" +
            "\r\n" +
            "-----1234--\r\n";
        // @formatter:on

        F upload = newFileUpload();
        upload.setFileSizeMax(-1);
        R req = newMockHttpServletRequest(request, null, null);
        List<FileItem> fileItems = upload.parseRequest(req);
        assertEquals(1, fileItems.size());
        FileItem item = fileItems.get(0);
        assertEquals("This is the content of the file\n", new String(item.get()));

        upload = newFileUpload();
        upload.setFileSizeMax(40);
        req = newMockHttpServletRequest(request, null, null);
        fileItems = upload.parseRequest(req);
        assertEquals(1, fileItems.size());
        item = fileItems.get(0);
        assertEquals("This is the content of the file\n", new String(item.get()));

        upload = newFileUpload();
        upload.setFileSizeMax(30);
        req = newMockHttpServletRequest(request, null, null);
        try {
            upload.parseRequest(req);
            fail("Expected exception.");
        } catch (final FileUploadByteCountLimitException e) {
            assertEquals(30, e.getPermitted());
        }
    }

    /**
     * Checks, whether a faked Content-Length header is detected.
     */
    @Test
    public void testFileSizeLimitWithFakedContentLength() throws IOException, FileUploadException {
        // @formatter:off
        final String request =
            "-----1234\r\n" +
            "Content-Disposition: form-data; name=\"file\"; filename=\"foo.tab\"\r\n" +
            "Content-Type: text/whatever\r\n" +
            "Content-Length: 10\r\n" +
            "\r\n" +
            "This is the content of the file\n" +
            "\r\n" +
            "-----1234--\r\n";
        // @formatter:on

        F upload = newFileUpload();
        upload.setFileSizeMax(-1);
        R req = newMockHttpServletRequest(request, null, null);
        List<FileItem> fileItems = upload.parseRequest(req);
        assertEquals(1, fileItems.size());
        FileItem item = fileItems.get(0);
        assertEquals("This is the content of the file\n", new String(item.get()));

        upload = newFileUpload();
        upload.setFileSizeMax(40);
        req = newMockHttpServletRequest(request, null, null);
        fileItems = upload.parseRequest(req);
        assertEquals(1, fileItems.size());
        item = fileItems.get(0);
        assertEquals("This is the content of the file\n", new String(item.get()));

        // provided Content-Length is larger than the FileSizeMax -> handled by ctor
        upload = newFileUpload();
        upload.setFileSizeMax(5);
        req = newMockHttpServletRequest(request, null, null);
        try {
            upload.parseRequest(req);
            fail("Expected exception.");
        } catch (final FileUploadByteCountLimitException e) {
            assertEquals(5, e.getPermitted());
        }

        // provided Content-Length is wrong, actual content is larger -> handled by LimitedInputStream
        upload = newFileUpload();
        upload.setFileSizeMax(15);
        req = newMockHttpServletRequest(request, null, null);
        try {
            upload.parseRequest(req);
            fail("Expected exception.");
        } catch (final FileUploadByteCountLimitException e) {
            assertEquals(15, e.getPermitted());
        }
    }

    /**
     * Checks whether maxSize works.
     */
    @Test
    public void testMaxSizeLimit() throws IOException, FileUploadException {
        // @formatter:off
        final String request =
            "-----1234\r\n" +
            "Content-Disposition: form-data; name=\"file1\"; filename=\"foo1.tab\"\r\n" +
            "Content-Type: text/whatever\r\n" +
            "Content-Length: 10\r\n" +
            "\r\n" +
            "This is the content of the file\n" +
            "\r\n" +
            "-----1234\r\n" +
            "Content-Disposition: form-data; name=\"file2\"; filename=\"foo2.tab\"\r\n" +
            "Content-Type: text/whatever\r\n" +
            "\r\n" +
            "This is the content of the file\n" +
            "\r\n" +
            "-----1234--\r\n";
        // @formatter:on

        final F upload = newFileUpload();
        upload.setFileSizeMax(-1);
        upload.setSizeMax(200);

        final R req = newMockHttpServletRequest(request, null, null);
        try {
            upload.parseRequest(req);
            fail("Expected exception.");
        } catch (final FileUploadSizeException e) {
            assertEquals(200, e.getPermitted());
        }
    }

    @Test
    public void testMaxSizeLimitUnknownContentLength() throws IOException, FileUploadException {
        // @formatter:off
        final String request =
            "-----1234\r\n" +
            "Content-Disposition: form-data; name=\"file1\"; filename=\"foo1.tab\"\r\n" +
            "Content-Type: text/whatever\r\n" +
            "Content-Length: 10\r\n" +
            "\r\n" +
            "This is the content of the file\n" +
            "\r\n" +
            "-----1234\r\n" +
            "Content-Disposition: form-data; name=\"file2\"; filename=\"foo2.tab\"\r\n" +
            "Content-Type: text/whatever\r\n" +
            "\r\n" +
            "This is the content of the file\n" +
            "\r\n" +
            "-----1234--\r\n";
        // @formatter:on

        final F upload = newFileUpload();
        upload.setFileSizeMax(-1);
        upload.setSizeMax(300);

        // the first item should be within the max size limit
        // set the read limit to 10 to simulate a "real" stream
        // otherwise the buffer would be immediately filled

        final R req = newMockHttpServletRequest(request, -1, 10);

        final FileItemIterator it = upload.getItemIterator(req);
        assertTrue(it.hasNext());

        final FileItemStream item = it.next();
        assertFalse(item.isFormField());
        assertEquals("file1", item.getFieldName());
        assertEquals("foo1.tab", item.getName());

        {
            try (final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    final InputStream stream = item.openStream()) {
                IOUtils.copy(stream, baos);
            }

        }

        // the second item is over the size max, thus we expect an error
        // the header is still within size max -> this shall still succeed
        assertTrue(it.hasNext());

        assertThrows(FileUploadException.class, () -> {
            final FileItemStream item2 = it.next();
            try (final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    final InputStream stream = item2.openStream()) {
                IOUtils.copy(stream, baos);
            }
        });
    }
}