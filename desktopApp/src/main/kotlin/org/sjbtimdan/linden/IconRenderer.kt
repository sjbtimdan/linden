package org.sjbtimdan.linden

import java.awt.BasicStroke
import java.awt.Color
import java.awt.GradientPaint
import java.awt.RadialGradientPaint
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

// Generates the master app icon from the design in art/linden-icon.svg.
// Run via ./gradlew :desktopApp:renderIcon
fun main() {
    val size = 1024
    val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g = img.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    g.paint = GradientPaint(
        0f,
        0f,
        Color(0x4CAF50),
        size.toFloat(),
        size.toFloat(),
        Color(0x1B5E20),
    )
    g.fill(RoundRectangle2D.Float(0f, 0f, size.toFloat(), size.toFloat(), 448f, 448f))

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

    g.dispose()

    val outDir = File("build/icon-render")
    outDir.mkdirs()
    val out = File(outDir, "linden-icon-1024.png")
    ImageIO.write(img, "png", out)
    println("wrote ${out.absolutePath}")
}
