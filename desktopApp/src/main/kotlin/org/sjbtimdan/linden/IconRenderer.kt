package org.sjbtimdan.linden

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.GradientPaint
import java.awt.Graphics2D
import java.awt.RadialGradientPaint
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

// Generates the master app icon from the design in
// src/main/composeResources/drawable/linden_icon.svg, plus the Google Play
// listing assets (512x512 icon, 1024x500 feature graphic) under docs/store-assets/.
// Run via ./gradlew :desktopApp:renderIcon
fun main() {
    System.setProperty("java.awt.headless", "true")

    val iconDir = File("build/icon-render")
    iconDir.mkdirs()
    val storeDir = File("docs/store-assets")
    storeDir.mkdirs()

    write(renderIcon(1024, rounded = true), File(iconDir, "linden-icon-1024.png"))
    write(renderIcon(512, rounded = false), File(storeDir, "play-icon-512.png"))
    write(renderFeatureGraphic(1024, 500), File(storeDir, "play-feature-graphic-1024x500.png"))
}

private fun write(image: BufferedImage, file: File) {
    ImageIO.write(image, "png", file)
    println("wrote ${file.absolutePath}")
}

private fun renderIcon(size: Int, rounded: Boolean): BufferedImage {
    val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g = img.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.scale(size / 1024.0, size / 1024.0)

    g.paint = GradientPaint(0f, 0f, Color(0x4CAF50), 1024f, 1024f, Color(0x1B5E20))
    if (rounded) {
        g.fill(RoundRectangle2D.Float(0f, 0f, 1024f, 1024f, 448f, 448f))
    } else {
        g.fill(Rectangle2D.Float(0f, 0f, 1024f, 1024f))
    }

    drawMark(g)
    g.dispose()
    return img
}

private fun renderFeatureGraphic(width: Int, height: Int): BufferedImage {
    val img = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val g = img.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

    g.paint = GradientPaint(0f, 0f, Color(0x4CAF50), width.toFloat(), height.toFloat(), Color(0x1B5E20))
    g.fillRect(0, 0, width, height)

    val saved = g.transform
    g.translate(270.0, height / 2.0)
    g.scale(0.42, 0.42)
    g.translate(-512.0, -512.0)
    drawMark(g)
    g.transform = saved

    g.font = Font("SansSerif", Font.BOLD, 104)
    g.color = Color.WHITE
    g.drawString("Linden", 470, 250)

    g.font = Font("SansSerif", Font.PLAIN, 30)
    g.color = Color(255, 255, 255, 224)
    g.drawString("Simple, private expense tracking", 470, 306)

    g.dispose()
    return img
}

private fun drawMark(g: Graphics2D) {
    g.color = Color.WHITE
    g.fill(Ellipse2D.Float(512f - 290f, 512f - 290f, 580f, 580f))

    // Coin: dark rim edge
    g.color = Color(0xB8860B)
    g.fill(Ellipse2D.Float(512f - 150f, 660f - 150f, 300f, 300f))

    // Coin: gold face (radial gradient for metallic sheen)
    g.paint = RadialGradientPaint(
        512f,
        660f - 40f,
        138f,
        floatArrayOf(0f, 0.6f, 1f),
        arrayOf(Color(0xFFD54F), Color(0xF5B301), Color(0xE0A800)),
    )
    g.fill(Ellipse2D.Float(512f - 138f, 660f - 138f, 276f, 276f))

    // Coin: rim highlight
    g.color = Color(0xC98A00)
    g.stroke = BasicStroke(5f)
    g.draw(Ellipse2D.Float(512f - 138f, 660f - 138f, 276f, 276f))

    // Coin: inner detail ring
    g.color = Color(0xE0A800)
    g.stroke = BasicStroke(7f)
    g.draw(Ellipse2D.Float(512f - 98f, 660f - 98f, 196f, 196f))

    // Tree trunk
    g.color = Color(0x795548)
    g.stroke = BasicStroke(26f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
    g.drawLine(512, 505, 512, 430)

    // Tree canopy: rounded crown of overlapping leaves
    g.color = Color(0x43A047)
    g.fill(Ellipse2D.Float(512f - 82f, 360f - 82f, 164f, 164f))
    g.fill(Ellipse2D.Float(450f - 54f, 388f - 54f, 108f, 108f))
    g.fill(Ellipse2D.Float(574f - 54f, 388f - 54f, 108f, 108f))
    g.fill(Ellipse2D.Float(512f - 50f, 308f - 50f, 100f, 100f))

    // Canopy highlights
    g.color = Color(0x66BB6A)
    g.fill(Ellipse2D.Float(512f - 46f, 342f - 46f, 92f, 92f))
    g.fill(Ellipse2D.Float(478f - 30f, 372f - 30f, 60f, 60f))
    g.fill(Ellipse2D.Float(546f - 30f, 372f - 30f, 60f, 60f))
}
