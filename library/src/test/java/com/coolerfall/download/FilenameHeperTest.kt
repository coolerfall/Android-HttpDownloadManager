package com.coolerfall.download

import org.junit.Assert
import org.junit.Test

/**
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
class FilenameHeperTest {

    @Test
    fun testFilename() {
        val filename = FilenameHelper.parseFilename(
            "http://the.url.com",
            "attachment; filename*=UTF-8''Na%C3%AFve%20file.txt",
            null
        )

        Assert.assertEquals("Na_ve_file.txt", filename)
    }
}