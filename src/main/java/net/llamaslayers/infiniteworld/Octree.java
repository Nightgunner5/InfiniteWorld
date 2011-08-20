package net.llamaslayers.infiniteworld;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.google.common.primitives.UnsignedBytes;
import java.io.Serializable;
import java.util.Arrays;

public class Octree implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final Interner<Octree> interner = Interners.newWeakInterner();
	private final Material content;
	private final byte lighting;
	/**
	 * 0 ( 0, 0, 0 )
	 * 1 ( 0, 0, 1 )
	 * 2 ( 0, 1, 0 )
	 * 3 ( 0, 1, 1 )
	 * 4 ( 1, 0, 0 )
	 * 5 ( 1, 0, 1 )
	 * 6 ( 1, 1, 0 )
	 * 7 ( 1, 1, 1 )
	 */
	private final Octree[] contents;

	private Octree(Material content) {
		contents = null;
		lighting = 0;
		this.content = content == Material.NOTHING ? null : content;
	}

	private Octree(Material[] contents) {
		if (contents.length != 8) {
			throw new ArrayIndexOutOfBoundsException();
		}
		if (contents[0] == contents[1]
				&& contents[1] == contents[2]
				&& contents[2] == contents[3]
				&& contents[3] == contents[4]
				&& contents[4] == contents[5]
				&& contents[5] == contents[6]
				&& contents[6] == contents[7]) {
			if (contents[0] == null || contents[0] == Material.NOTHING) {
				this.contents = null;
				content = null;
				lighting = 0;
				return;
			}
			this.contents = null;
			content = contents[0];
			lighting = 0;
			return;
		}
		this.contents = new Octree[8];
		for (int i = 0; i < 8; i++) {
			this.contents[i] = interner.intern(new Octree(contents[i]));
		}
		content = null;
		lighting = 0;
	}

	private Octree(byte[] lighting) {
		content = null;
		if (lighting[0] == lighting[1]
				&& lighting[1] == lighting[2]
				&& lighting[2] == lighting[3]
				&& lighting[3] == lighting[4]
				&& lighting[4] == lighting[5]
				&& lighting[5] == lighting[6]
				&& lighting[6] == lighting[7]) {
			this.lighting = lighting[0];
			this.contents = null;
			return;
		}
		this.lighting = 0;
		contents = new Octree[8];
		for (int i = 0; i < 8; i++) {
			contents[i] = interner.intern(new Octree(lighting[i]));
		}
	}

	private Octree(Octree... contents) {
		if (contents.length != 8) {
			throw new ArrayIndexOutOfBoundsException();
		}
		for (int i = 0; i < 8; i++) {
			contents[i] = interner.intern(contents[i]);
		}
		if (contents[0].contents == null
				&& contents[0] == contents[1]
				&& contents[1] == contents[2]
				&& contents[2] == contents[3]
				&& contents[3] == contents[4]
				&& contents[4] == contents[5]
				&& contents[5] == contents[6]
				&& contents[6] == contents[7]) {
			this.contents = null;
			content = contents[0].content;
			lighting = contents[0].lighting;
			return;
		}
		this.contents = contents;
		content = null;
		lighting = 0;
	}

	private Octree(byte lighting) {
		this.lighting = lighting;
		contents = null;
		content = null;
	}

	private Octree() {
		content = null;
		contents = null;
		lighting = 0;
	}

	public static Octree generateOctree(OctreeGenerator gen, int detail,
										double startX, double startY, double startZ,
										double endX, double endY, double endZ) {
		double halfX = (endX - startX) / 2;
		double halfY = (endY - startY) / 2;
		double halfZ = (endZ - startZ) / 2;
		if (detail > 0) {
			return interner.intern(new Octree(generateOctree(gen, detail - 1, startX, startY, startZ, endX - halfX, endY - halfY, endZ - halfZ),
											  generateOctree(gen, detail - 1, startX, startY, startZ + halfZ, endX - halfX, endY - halfY, endZ),
											  generateOctree(gen, detail - 1, startX, startY + halfY, startZ, endX - halfX, endY, endZ - halfZ),
											  generateOctree(gen, detail - 1, startX, startY + halfY, startZ + halfZ, endX - halfX, endY, endZ),
											  generateOctree(gen, detail - 1, startX + halfX, startY, startZ, endX, endY - halfY, endZ - halfZ),
											  generateOctree(gen, detail - 1, startX + halfX, startY, startZ + halfZ, endX, endY - halfY, endZ),
											  generateOctree(gen, detail - 1, startX + halfX, startY + halfY, startZ, endX, endY, endZ - halfZ),
											  generateOctree(gen, detail - 1, startX + halfX, startY + halfY, startZ + halfZ, endX, endY, endZ)));
		}

		halfX /= 2;
		halfY /= 2;
		halfZ /= 2;

		return interner.intern(new Octree(new Material[] {
					gen.getMaterial(startX + halfX, startY + halfY, startZ + halfZ),
					gen.getMaterial(startX + halfX, startY + halfY, endZ - halfZ),
					gen.getMaterial(startX + halfX, endY - halfY, startZ + halfZ),
					gen.getMaterial(startX + halfX, endY - halfY, endZ - halfZ),
					gen.getMaterial(endX - halfX, startY + halfY, startZ + halfZ),
					gen.getMaterial(endX - halfX, startY + halfY, endZ - halfZ),
					gen.getMaterial(endX - halfX, endY - halfY, startZ + halfZ),
					gen.getMaterial(endX - halfX, endY - halfY, endZ - halfZ)
				}));
	}

	/**
	 * x, y, and z are 0..1
	 */
	public int getLightAt(double x, double y, double z) {
		if (contents == null) {
			return UnsignedBytes.toInt(lighting);
		}

		int coord = (x >= 0.5 ? 4 : 0) | (y >= 0.5 ? 2 : 0) | (z >= 0.5 ? 1 : 0);

		Octree oct = contents[coord];

		return oct.getLightAt((x * 2) % 1, (y * 2) % 1, (z * 2) % 1);
	}

	/**
	 * x, y, and z are 0..1
	 */
	public Material getMaterialAt(double x, double y, double z) {
		if (contents == null) {
			return content == null ? Material.NOTHING : content;
		}

		int coord = (x >= 0.5 ? 4 : 0) | (y >= 0.5 ? 2 : 0) | (z >= 0.5 ? 1 : 0);

		Octree oct = contents[coord];

		return oct.getMaterialAt((x * 2) % 1, (y * 2) % 1, (z * 2) % 1);
	}
	private static final int CLIP_RIGHT = 1 << 0;	// cohen-sutherland clipping outcodes
	private static final int CLIP_LEFT = 1 << 1;
	private static final int CLIP_TOP = 1 << 2;
	private static final int CLIP_BOTTOM = 1 << 3;
	private static final int CLIP_FRONT = 1 << 4;
	private static final int CLIP_BACK = 1 << 5;

	// Based on code from http://www.gdmag.com/src/aug01.zip
	private static int calcOutcode(double x, double y, double z,
								   double minX, double minY, double minZ,
								   double maxX, double maxY, double maxZ) {
		int outcode = 0;

		if (x > maxX) {
			outcode |= CLIP_RIGHT;
		} else if (x < minX) {
			outcode |= CLIP_LEFT;
		}
		if (y > maxY) {
			outcode |= CLIP_TOP;
		} else if (y < minY) {
			outcode |= CLIP_BOTTOM;
		}
		if (z > maxZ) {
			outcode |= CLIP_BACK;
		} else if (z < minZ) {
			outcode |= CLIP_FRONT;
		}

		return outcode;
	}

	private static double[] doesCollide(double startX, double startY, double startZ,
										double endX, double endY, double endZ,
										double minX, double minY, double minZ,
										double maxX, double maxY, double maxZ) {
		int outcode1 = calcOutcode(startX, startY, startZ, minX, minY, minZ, maxX, maxY, maxZ);
		if (outcode1 == 0) {
			// point inside bounding box
			return new double[] {startX, startY, startZ};
		}

		int outcode2 = calcOutcode(endX, endY, endZ, minX, minY, minZ, maxX, maxY, maxZ);
		if ((outcode1 & outcode2) > 0) {
			// both points on same side of box
			return null;
		}

		// check intersections
		if ((outcode1 & (CLIP_RIGHT | CLIP_LEFT)) != 0) {
			double interceptX;
			if ((outcode1 & CLIP_RIGHT) != 0) {
				interceptX = maxX;
			} else {
				interceptX = minX;
			}
			double x1 = endX - startX;
			double x2 = interceptX - startX;
			double interceptY = startY + x2 * (endY - startY) / x1;
			double interceptZ = startZ + x2 * (endZ - startZ) / x1;

			if (interceptY <= maxY && interceptY >= minY && interceptZ <= maxZ && interceptZ >= minZ) {
				return new double[] {interceptX, interceptY, interceptZ};
			}
		}

		if ((outcode1 & (CLIP_TOP | CLIP_BOTTOM)) != 0) {
			double interceptY;
			if ((outcode1 & CLIP_TOP) != 0) {
				interceptY = maxY;
			} else {
				interceptY = minY;
			}
			double y1 = endY - startY;
			double y2 = interceptY - startY;
			double interceptX = startX + y2 * (endX - startX) / y1;
			double interceptZ = startZ + y2 * (endZ - startZ) / y1;

			if (interceptX <= maxX && interceptX >= minX && interceptZ <= maxZ && interceptZ >= minZ) {
				return new double[] {interceptX, interceptY, interceptZ};
			}
		}

		if ((outcode1 & (CLIP_FRONT | CLIP_BACK)) != 0) {
			double interceptZ;
			if ((outcode1 & CLIP_BACK) != 0) {
				interceptZ = maxZ;
			} else {
				interceptZ = minZ;
			}
			double z1 = endZ - startZ;
			double z2 = interceptZ - startZ;
			double interceptX = startX + z2 * (endX - startX) / z1;
			double interceptY = startY + z2 * (endY - startY) / z1;

			if (interceptX <= maxX && interceptX >= minX && interceptY <= maxY && interceptY >= minY) {
				return new double[] {interceptX, interceptY, interceptZ};
			}
		}

		return null;
	}

	/**
	 * x, y, and z are 0..1
	 * Returns null if nothing is hit
	 */
	public double[] trace(double startX, double startY, double startZ,
						  double endX, double endY, double endZ) {
		if (contents == null) {
			return content == null ? null : doesCollide(startX, startY, startZ, endX, endY, endZ,
														0, 0, 0, 1, 1, 1);
		}

		for (int _x = 0; _x < 2; _x++) {
			for (int _y = 0; _y < 2; _y++) {
				for (int _z = 0; _z < 2; _z++) {
					int x = startX > endX ? 1 - _x : _x;
					int y = startY > endY ? 1 - _y : _y;
					int z = startZ > endZ ? 1 - _z : _z;
					if (doesCollide(startX, startY, startZ, endX, endY, endZ,
									x * 0.5, y * 0.5, z * 0.5,
									x * 0.5 + 0.5, y * 0.5 + 0.5, z * 0.5 + 0.5) != null) {
						double[] pos = contents[x * 4 + y * 2 + z].trace(startX * 2 - x, startY * 2 - y, startZ * 2 - z,
																		 endX * 2 - x, endY * 2 - y, endZ * 2 - z);
						if (pos != null) {
							pos[0] = pos[0] / 2 + x * 0.5;
							pos[1] = pos[1] / 2 + y * 0.5;
							pos[2] = pos[2] / 2 + z * 0.5;
							return pos;
						}
					}
				}
			}
		}

		return null;
	}

	/**
	 * x, y, and z are 0..1
	 * Returns null if nothing is hit
	 */
	public double[] traceFov(double startX, double startY, double startZ,
							 double minX, double minY, double minZ,
							 double endX, double endY, double endZ,
							 double maxX, double maxY, double maxZ) {
		double[] near = doesCollide(startX, startY, startZ, endX, endY, endZ, minX, minY, minZ, maxX, maxY, maxZ);
		if (near == null) {
			return null;
		}

		double[] far = doesCollide(endX, endY, endZ, startX, startY, startZ, minX, minY, minZ, maxX, maxY, maxZ);
		if (far == null) {
			throw new RuntimeException();
		}

		return trace(near[0], near[1], near[2], far[0], far[1], far[2]);
	}

	private Octree _generateLightingOctree(byte[][][] lighting,
										   int startX, int startY, int startZ,
										   int endX, int endY, int endZ) {
		if (endX == startX + 1) {
			return interner.intern(new Octree(new byte[] {
						lighting[startX][startY][startZ],
						lighting[startX][startY][endZ],
						lighting[startX][endY][startZ],
						lighting[startX][endY][endZ],
						lighting[endX][startY][startZ],
						lighting[endX][startY][endZ],
						lighting[endX][endY][startZ],
						lighting[endX][endY][endZ]
					}));
		}

		int half = (endX - startX) / 2;

		return interner.intern(new Octree(
				_generateLightingOctree(lighting, startX, startY, startZ, endX - half, endY - half, endZ - half),
				_generateLightingOctree(lighting, startX, startY, startZ + half, endX - half, endY - half, endZ),
				_generateLightingOctree(lighting, startX, startY + half, startZ, endX - half, endY, endZ - half),
				_generateLightingOctree(lighting, startX, startY + half, startZ + half, endX - half, endY, endZ),
				_generateLightingOctree(lighting, startX + half, startY, startZ, endX, endY - half, endZ - half),
				_generateLightingOctree(lighting, startX + half, startY, startZ + half, endX, endY - half, endZ),
				_generateLightingOctree(lighting, startX + half, startY + half, startZ, endX, endY, endZ - half),
				_generateLightingOctree(lighting, startX + half, startY + half, startZ + half, endX, endY, endZ)));
	}

	public Octree generateLightingOctree(int detail) {
		byte[][][] lighting = new byte[1 << detail][1 << detail][1 << detail];

		double step = 1.0 / (1 << detail);

		for (int x = 0; x < (1 << detail); x++) {
			for (int y = 0; y < (1 << detail); y++) {
				for (int z = 0; z < (1 << detail); z++) {
					if (getMaterialAt(x * step, y * step, z * step) == Material.NOTHING) {
						continue;
					}
					double[] lightingTrace = trace(x * step, -3, z * step, x * step, y * step, z * step);
					lightingTrace[0] -= x * step;
					lightingTrace[1] -= y * step;
					lightingTrace[2] -= z * step;
					double distance = Math.sqrt(lightingTrace[0] * lightingTrace[0]
							+ lightingTrace[1] * lightingTrace[1]
							+ lightingTrace[2] * lightingTrace[2]);

					lighting[x][y][z] = (byte) (255 - Math.min(Math.max((long) (distance * 2000), 0), 255));
				}
			}
		}

		final double[][] aoTraces = {
			{2, 0, 0},
			{0, 2, 0},
			{0, 0, 2},
			{-2, 0, 0},
			{0, -2, 0},
			{0, 0, -2}
		};

		for (int x = 2; x < (1 << detail) - 2; x++) {
			for (int y = 2; y < (1 << detail) - 2; y++) {
				for (int z = 2; z < (1 << detail) - 2; z++) {
					if (getMaterialAt(x * step, y * step, z * step) != Material.NOTHING) {
						continue;
					}
					for (double[] aoTrace : aoTraces) {
						double[] traceResult = trace(x * step, y * step, z * step,
													 (x + aoTrace[0]) * step,
													 (y + aoTrace[1]) * step,
													 (z + aoTrace[2]) * step);
						if (traceResult == null) {
							for (int _x = x - 2; _x <= x + 2; _x++) {
								for (int _y = y - 2; _y <= y + 2; _y++) {
									for (int _z = z - 2; _z <= z + 2; _z++) {
										int light = UnsignedBytes.toInt(lighting[x][y][z]);
										light += 2;
										lighting[x][y][z] = UnsignedBytes.saturatedCast(light);
									}
								}
							}
						}
					}
				}
			}
		}

		return _generateLightingOctree(lighting, 0, 0, 0, (1 << detail) - 1, (1 << detail) - 1, (1 << detail) - 1);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Octree other = (Octree) obj;
		if (this.content != other.content) {
			return false;
		}
		if (this.lighting != other.lighting) {
			return false;
		}
		if (!Arrays.deepEquals(this.contents, other.contents)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 37 * hash + (content != null ? content.hashCode() : 0);
		hash = 37 * hash + lighting;
		hash = 37 * hash + Arrays.deepHashCode(contents);
		return hash;
	}
}
