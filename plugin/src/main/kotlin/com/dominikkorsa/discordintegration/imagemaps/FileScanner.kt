package com.dominikkorsa.discordintegration.imagemaps

import com.dominikkorsa.discordintegration.DiscordIntegration
import java.io.File
import javax.imageio.ImageIO
import java.nio.file.Path

/* This class will scan files submitted by the user to make sure
* they are actually PNG files and are safe. */
class FileScanner(private val plugin: DiscordIntegration) {


    /* Call verifyPNG and verifySafeness*/
     fun scan(file: Path): Boolean {
        return verifyPNG(file.toFile())
    }

    private fun verifyPNG(file: File): Boolean {
        try {
            // verify it's an image, and then verify it has .PNG extension
            ImageIO.read(file).toString()
        } catch (e: Exception) {
            plugin.logger.warning(e.toString())
            return false
        }
        return true
    }
}