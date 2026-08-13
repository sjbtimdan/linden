package org.sjbtimdan.linden

import java.awt.BasicStroke
import java.awt.Color
import java.awt.GradientPaint
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
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
        0f, 0f, Color(0x4CAF50),
        size.toFloat(), size.toFloat(), Color(0x1B5E20),
    )
    g.fill(RoundRectangle2D.Float(0f, 0f, size.toFloat(), size.toFloat(), 448f, 448f))

    g.color = Color.WHITE
    g.fill(Ellipse2D.Float(512f - 290f, 512f - 290f, 580f, 580f))

    g.color = Color(0x1B5E20)
    g.stroke = BasicStroke(24f)
    g.draw(Ellipse2D.Float(512f - 220f, 512f - 220f, 440f, 440f))

    g.color = Color(0x43A047)
    val leaf = Path2D.Float()
    leaf.moveTo(512.0, 645.2)
    leaf.curveTo(434.25, 600.94, 356.32, 545.38, 356.32, 467.53)
    leaf.curveTo(356.32, 411.97, 395.57, 378.03, 440.04, 378.03)
    leaf.curveTo(467.53, 378.03, 500.91, 406.09, 512.0, 468.84)
    leaf.curveTo(523.09, 406.09, 556.47, 378.03, 583.96, 378.03)
    leaf.curveTo(628.43, 378.03, 667.68, 411.97, 667.68, 467.53)
    leaf.curveTo(667.68, 545.38, 589.75, 600.94, 512.0, 645.2)
    leaf.closePath()
    g.fill(leaf)

    g.fill(RoundRectangle2D.Float(501.29f, 644.17f, 21.42f, 38.12f, 21.42f, 21.42f))
    g.dispose()

    val outDir = File("build/icon-render")
    outDir.mkdirs()
    val out = File(outDir, "linden-icon-1024.png")
    ImageIO.write(img, "png", out)
    println("wrote ${out.absolutePath}")
}
