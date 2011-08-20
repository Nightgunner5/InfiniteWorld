package net.llamaslayers.infiniteworld;

import org.bukkit.util.noise.SimplexNoiseGenerator;

class RawSimplexGen implements OctreeGenerator {
	private static final SimplexNoiseGenerator noise = SimplexNoiseGenerator.getInstance();

	public Material getMaterial(float x, float y, float z) {
		return noise.noise(x / 128, y / 128, z / 128, 8, 0.5, 0.5) > 0 ? Material.STUFF : Material.NOTHING;
	}
}
