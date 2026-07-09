package ru.nikit.megastructure.world;

final class MegastructureMath {
	private MegastructureMath() {
	}

	static int floorMod(int value, int divisor) {
		int result = value % divisor;
		return result < 0 ? result + divisor : result;
	}

	static int floorDiv(int value, int divisor) {
		int result = value / divisor;
		if ((value ^ divisor) < 0 && result * divisor != value) {
			result--;
		}
		return result;
	}

	static long hash(long seed, int x, int z, int salt) {
		long h = seed;
		h ^= x * 0x9E3779B97F4A7C15L;
		h ^= z * 0xC2B2AE3D27D4EB4FL;
		h ^= salt * 0x165667B19E3779F9L;
		h ^= h >>> 33;
		h *= 0xff51afd7ed558ccdL;
		h ^= h >>> 33;
		h *= 0xc4ceb9fe1a85ec53L;
		h ^= h >>> 33;
		return h;
	}

	static boolean chance(long hash, int oneIn) {
		return Math.floorMod(hash, oneIn) == 0;
	}

	static int range(long hash, int min, int maxInclusive) {
		int bound = maxInclusive - min + 1;
		return min + Math.floorMod((int) (hash ^ (hash >>> 32)), bound);
	}

	static int manhattanToCellEdge(int localX, int localZ, int cellSize) {
		int edgeX = Math.min(localX, cellSize - 1 - localX);
		int edgeZ = Math.min(localZ, cellSize - 1 - localZ);
		return Math.min(edgeX, edgeZ);
	}
}
