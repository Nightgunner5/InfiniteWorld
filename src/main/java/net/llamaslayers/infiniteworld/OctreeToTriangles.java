package net.llamaslayers.infiniteworld;

import com.google.common.collect.Lists;
import com.google.common.primitives.Doubles;
import java.util.ArrayList;

/**
 * @author Nightgunner5
 */
class OctreeToTriangles {
	// 0..15
	private static final byte[][] standingStillCubes = new byte[256][];
	static {
		byte[] b = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
		int[][][] corners = {{{128, 64}, {32, 16}}, {{8, 4}, {2, 1}}};
		for (int i = 0; i < 256; i++) {
			standingStillCubes[i] = new byte[0];
			/*if (i == 255 || i == 0)
				continue;
			for (int x = 0; x < 2; x++) {
				for (int y = 0; y < 2; y++) {
					for (int z = 0; z < 2; z++) {
						int corner = corners[x][y][z];
						int corner1 = corners[x ^ 1][y][z];
						int corner2 = corners[x][y ^ 1][z];
						int corner3 = corners[x][y][z ^ 1];
						if ((i & corner) == corner && (i & (corner1 | corner2 | corner3)) == 0)
							standingStillCubes[i] = realloc(standingStillCubes[i],
															x == 0 ? b[0] : b[15],
															y == 0 ? b[4] : b[11],
															z == 0 ? b[4] : b[11],
															x == 0 ? b[4] : b[11],
															y == 0 ? b[0] : b[15],
															z == 0 ? b[4] : b[11],
															x == 0 ? b[4] : b[11],
															y == 0 ? b[4] : b[11],
															z == 0 ? b[0] : b[15]);
					}
				}
			}*/
			if ((i & 1) == 1) {
				standingStillCubes[i] = new byte[] {
					0, 0, 0,
					15, 0, 0,
					15, 15, 0
				};
			}
		}
	}

	private static byte[] realloc(byte[] in, byte... add) {
		byte[] out = new byte[in.length + add.length];
		System.arraycopy(in, 0, out, 0, in.length);
		System.arraycopy(add, 0, out, in.length, add.length);
		return out;
	}

	public static double[] convert(Octree oct, int detail,
								   double startX, double startY, double startZ,
								   double endX, double endY, double endZ) {
		ArrayList<Double> coords = Lists.newArrayList();
		double inc = 1.0 / (1 << detail);

		for (double x = inc / 2; x < 1; x += inc) {
			for (double y = inc / 2; y < 1; y += inc) {
				for (double z = inc / 2; z < 1; z += inc) {
					int cubeType = (oct.getMaterialAt(x - inc, y - inc, z - inc) != null ? 128 : 0)
							| (oct.getMaterialAt(x - inc, y - inc, z) != null ? 64 : 0)
							| (oct.getMaterialAt(x - inc, y, z - inc) != null ? 32 : 0)
							| (oct.getMaterialAt(x - inc, y, z) != null ? 16 : 0)
							| (oct.getMaterialAt(x, y - inc, z - inc) != null ? 8 : 0)
							| (oct.getMaterialAt(x, y - inc, z) != null ? 4 : 0)
							| (oct.getMaterialAt(x, y, z - inc) != null ? 2 : 0)
							| (oct.getMaterialAt(x, y, z) != null ? 1 : 0);

					int i = 0;
					for (byte vert : standingStillCubes[cubeType]) {
						coords.add(convertVert(detail, x, y, z, i, vert, startX, startY, startZ, endX, endY, endZ));

						i++;
						i %= 3;
					}
				}
			}
		}

		return Doubles.toArray(coords);
	}

	private static double convertVert(int detail, double x, double y, double z, int i, byte vert,
									  double startX, double startY, double startZ,
									  double endX, double endY, double endZ) {
		double _start = i == 0 ? startX : (i == 1 ? startY : startZ);
		double _end = i == 0 ? endX : (i == 1 ? endY : endZ);
		double start = _start + (i == 0 ? x : (i == 1 ? y : z));
		double inc = (_end - _start) / (1 << detail);
		return start + (vert / 15.0 * inc);
	}
}
