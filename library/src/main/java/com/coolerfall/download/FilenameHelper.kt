package com.coolerfall.download

import android.net.Uri
import android.webkit.MimeTypeMap
import java.net.URLDecoder
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
object FilenameHelper {

    /** The default base name for downloaded files if we can't get one at the HTTP level  */
    private const val DEFAULT_DL_FILENAME = "down"

    /** The default extension for html files if we can't get one at the HTTP level  */
    private const val DEFAULT_DL_HTML_EXTENSION = ".html"

    /** The default extension for text files if we can't get one at the HTTP level  */
    private const val DEFAULT_DL_TEXT_EXTENSION = ".txt"

    /** The default extension for binary files if we can't get one at the HTTP level  */
    private const val DEFAULT_DL_BINARY_EXTENSION = ".bin"

    private val CONTENT_DISPOSITION_PATTERN: Pattern =
        Pattern.compile("attachment;\\s*filename\\*?=['\"]?(?:UTF-\\d['\"]*)?([^;\\r\\n\"']*)['\"]?;?")

    /**
     * Parse file from url content-disposition header and mime type
     *
     * @param url url
     * @param contentDisposition content disposition header
     * @param mimeType mime type
     */
    fun parseFilename(url: String, contentDisposition: String?, mimeType: String?): String {
        var filename = chooseFilename(url, contentDisposition)
        val extension: String?
        val dotIndex = filename.lastIndexOf('.')
        if (dotIndex < 0) {
            extension = chooseExtensionFromMimeType(mimeType, true)
        } else {
            extension = chooseExtensionFromFilename(mimeType, filename, dotIndex)
            filename = filename.substring(0, dotIndex)
        }

        return filename + extension
    }

    /**
     * Parse content-disposition header to get filename.
     *
     * @param url url
     * @param contentDisposition content disposition header
     */
    private fun chooseFilename(url: String, contentDisposition: String?): String {
        var filename: String? = null
        /* If we couldn't do anything with the hint, move toward the content disposition */
        if (contentDisposition != null) {
            filename = parseContentDisposition(contentDisposition)
            if (filename != null) {
                val index = filename.lastIndexOf('/') + 1
                if (index > 0) {
                    filename = filename.substring(index)
                }
            }
        }
        /* If all the other http-related approaches failed, use the plain uri */
        if (filename == null) {
            val decodedUrl = Uri.decode(url)
            if (decodedUrl != null && !decodedUrl.endsWith("/") && decodedUrl.indexOf('?') < 0) {
                val index = decodedUrl.lastIndexOf('/') + 1
                if (index > 0) {
                    filename = decodedUrl.substring(index)
                }
            }
        }
        /* Finally, if couldn't get filename from URI, get a generic filename */
        if (filename == null) {
            filename = DEFAULT_DL_FILENAME
        }

        return filename.replace("[^a-zA-Z0-9\\.\\-_]+".toRegex(), "_")
    }

    /*
     * Parse the Content-Disposition HTTP Header. The format of the header
     * is defined here: http://www.w3.org/Protocols/rfc2616/rfc2616-sec19.html
     * This header provides a filename for content that is going to be
     * downloaded to the file system. We only support the attachment type.
     */
    private fun parseContentDisposition(contentDisposition: String): String? {
        try {
            val m: Matcher = CONTENT_DISPOSITION_PATTERN.matcher(contentDisposition)
            if (m.find()) {
                return URLDecoder.decode(m.group(1), "utf-8")
            }
        } catch (ignore: IllegalStateException) {
        }
        return null
    }

    private fun chooseExtensionFromMimeType(mimeType: String?, useDefaults: Boolean): String? {
        var extension: String? = null
        if (mimeType != null) {
            extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            if (extension != null) {
                extension = ".$extension"
            }
        }
        if (extension != null) {
            return extension
        }

        if (mimeType != null && mimeType.lowercase(Locale.getDefault()).startsWith("text/")) {
            if (mimeType.equals("text/html", ignoreCase = true)) {
                extension = DEFAULT_DL_HTML_EXTENSION
            } else if (useDefaults) {
                extension = DEFAULT_DL_TEXT_EXTENSION
            }
        } else if (useDefaults) {
            extension = DEFAULT_DL_BINARY_EXTENSION
        }
        return extension
    }

    private fun chooseExtensionFromFilename(
        mimeType: String?,
        filename: String,
        dotIndex: Int
    ): String {
        var extension: String? = null
        if (mimeType != null) {
            /* Compare the last segment of the extension against the mime type. */
            /* If there's a mismatch, discard the entire extension. */
            val lastDotIndex = filename.lastIndexOf('.')
            val typeFromExt = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                filename.substring(lastDotIndex + 1)
            )
            if (typeFromExt == null || !typeFromExt.equals(mimeType, ignoreCase = true)) {
                extension = chooseExtensionFromMimeType(mimeType, false)
            }
        }
        if (extension == null) {
            extension = filename.substring(dotIndex)
        }
        return extension
    }
}