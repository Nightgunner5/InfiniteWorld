package net.llamaslayers.infiniteworld;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

public class App {
	public static void main(String[] args) {
		System.out.println("Memory at start: " + getMem());
		OctreeGenerator gen = new RawSimplexGen();
		Octree oct = null;
		long start = 0, end = 0;

		System.out.println("Memory after initialization: " + getMem());
		System.gc();

		for (int detail = 1; detail < 16; detail++) {
			System.out.println("Memory before generating octree #" + detail + ": " + getMem());
			start = System.nanoTime();
			oct = Octree.generateOctree(gen, detail, -128, -128, -128, 128, 128, 128);
			end = System.nanoTime();
			System.out.println("   Octree generation took " + (end - start) / 1000000000.0 + " seconds.");
			System.out.println("Memory after generating octree #" + detail + ": " + getMem());

			System.out.println("Memory before processing lighting for octree #" + detail + ": " + getMem());
			start = System.nanoTime();
			Octree lightingOct = oct.generateLightingOctree(detail);
			end = System.nanoTime();
			System.out.println("   Lighting calculation took " + (end - start) / 1000000000.0 + " seconds.");
			System.out.println("Memory after processing lighting for octree #" + detail + ": " + getMem());

			{
				System.out.println("Memory before rendering octree #" + detail + ": " + getMem());
				start = System.nanoTime();
				BufferedImage img = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
				for (int x = 0; x < 512; x++) {
					for (int y = 0; y < 512; y++) {
						double[] pos = oct.traceFov(0.5, 0.5, -2, 0.25, 0.25, 0.25,
													x / 1024.0 + 0.25, y / 1024.0 + 0.25, 0.75,
													0.75, 0.75, 0.75);
						if (pos == null) {
							img.setRGB(x, y, 0x77EEFF);
						} else {
							img.setRGB(x, y, compostFog(lightingOct.getLightAt(pos[0], pos[1], pos[2]), Math.max(pos[2], 0.25)));
						}
					}
				}
				try {
					ImageIO.write(img, "png", new File(detail + ".png"));
				} catch (IOException ex) {
					Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
				}
				img.flush();
				end = System.nanoTime();
				System.out.println("   Rendering took " + (end - start) / 1000000000.0 + " seconds.");
				System.out.println("Memory after rendering octree #" + detail + ": " + getMem());
			}

			oct = null;
			lightingOct = null;
			System.gc();
			System.out.println("Memory after attempting to GC the octree: " + getMem());
		}
	}

	private static String getMem() {
		return NumberFormat.getIntegerInstance().format(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
	}

	private static int compostFog(int lighting, double fog) {
		fog = fog * 2 - 0.5;
		int r = (int) (lighting * (1 - fog) + 0x77 * fog);
		int g = (int) (lighting * (1 - fog) + 0xEE * fog);
		int b = (int) (lighting * (1 - fog) + 0xFF * fog);
		return (r << 16) | (g << 8) | b;
	}
}
