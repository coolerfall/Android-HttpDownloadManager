package com.coolerfall.download

import android.text.TextUtils
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID

/**
 * Contains some utils used in download manager.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
object Helper {
	const val DEFAULT_READ_TIMEOUT = 30 * 1000
	const val DEFAULT_WRITE_TIMEOUT = 30 * 1000
	const val DEFAULT_CONNECT_TIMEOUT = 30 * 1000
	const val HTTP = "http"
	const val HTTPS = "https"
	const val LOCATION = "Location"
	const val CONTENT_DISPOSITION = "Content-Disposition"
	const val MAX_REDIRECTION = 5
	const val HTTP_OK = 200
	const val HTTP_PARTIAL = 206
	const val HTTP_TEMP_REDIRECT = 307

	/* get uuid without '-' */
	private val uuid: String
		get() = UUID.randomUUID()
			.toString()
			.trim { it <= ' ' }
			.replace("-".toRegex(), "")

	/* calculate md5 for string */
	internal fun md5(origin: String): String {
		val md = MessageDigest.getInstance("MD5")
		md.update(origin.toByteArray(StandardCharsets.UTF_8))
		val bi = BigInteger(1, md.digest())
		val hash = StringBuilder(bi.toString(16))
		while (hash.length < 32) {
			hash.insert(0, "0")
		}
		return hash.toString()
	}

	/**
	 * Get filename from url.
	 *
	 * @param url url
	 * @return filename or md5 if no available filename
	 */
	private fun getFilenameFromUrl(url: String): String {
		var filename = md5(url) + ".file"
		val index = url.lastIndexOf("/")
		if (index > 0) {
			var tmpFilename = url.substring(index + 1)
			val qMarkIndex = tmpFilename.indexOf("?")
			if (qMarkIndex > 0) {
				tmpFilename = tmpFilename.substring(0, qMarkIndex - 1)
			}

			/* if filename contains '.', then the filename has file extension */
			if (tmpFilename.contains(".")) {
				filename = tmpFilename
			}
		}
		return filename
	}

	/**
	 * Get filename from content disposition in header.
	 *
	 * @param url url of current file to download
	 * @param contentDisposition content disposition in header
	 * @return filename in header if existed, otherwise get from url
	 */
	fun getFilenameFromHeader(url: String, contentDisposition: String?): String {
		val filename: String = if (!TextUtils.isEmpty(contentDisposition)) {
			val index = contentDisposition!!.indexOf("filename")
			if (index > 0) {
				return contentDisposition.substring(index + 9)
			} else {
				getFilenameFromUrl(url)
			}
		} else {
			getFilenameFromUrl(url)
		}

		return URLDecoder.decode(filename, "UTF-8")
	}

	/**
	 * Resolve filepath with given root path and relative path.
	 *
	 * @param path the root path
	 * @param other relative path
	 * @return full path
	 */
	internal fun resolvePath(path: String, other: String): String {
		return path + (if (path.endsWith(File.separator)) "" else File.separator) + other
	}

	/**
	 * Reads all bytes from an input stream and writes them to an output stream.
	 *
	 * @param source [InputStream]
	 * @param sink [OutputStream]
	 * @return copied bytes length
	 * @throws IOException if failed to copy stream
	 */
	@Throws(IOException::class) internal fun copy(source: InputStream, sink: OutputStream): Long {
		var nRead = 0L
		val buf = ByteArray(8092)
		var n: Int
		while (source.read(buf).also { n = it } > 0) {
			sink.write(buf, 0, n)
			nRead += n.toLong()
		}
		return nRead
	}

	/**
	 * Create default [Downloader] for download manager.
	 *
	 * @return [Downloader]
	 */
	internal fun createDefaultDownloader(): Downloader {
		return try {
			Class.forName("okhttp3.OkHttpClient")
			OkHttpDownloader.create()
		} catch (ignored: ClassNotFoundException) {
			URLDownloader.create()
		}
	}
}