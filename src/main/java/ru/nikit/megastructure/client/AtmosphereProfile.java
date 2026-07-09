package ru.nikit.megastructure.client;

enum AtmosphereProfile {
	SERVICE(0.34F, 0.37F, 0.36F, 0.050F, 0.62F),
	NETWORK(0.37F, 0.40F, 0.39F, 0.040F, 0.78F),
	TITAN(0.43F, 0.45F, 0.43F, 0.032F, 1.00F),
	ABYSS(0.25F, 0.28F, 0.29F, 0.043F, 0.92F),
	RIFT(0.39F, 0.41F, 0.40F, 0.036F, 1.00F),
	OASIS(0.36F, 0.42F, 0.37F, 0.034F, 0.86F),
	RAIL(0.40F, 0.41F, 0.39F, 0.035F, 0.94F);

	final float red;
	final float green;
	final float blue;
	final float layerAlpha;
	final float reach;

	AtmosphereProfile(float red, float green, float blue, float layerAlpha, float reach) {
		this.red = red;
		this.green = green;
		this.blue = blue;
		this.layerAlpha = layerAlpha;
		this.reach = reach;
	}
}
