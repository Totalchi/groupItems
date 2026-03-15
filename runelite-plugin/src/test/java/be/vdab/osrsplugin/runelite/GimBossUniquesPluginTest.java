package be.vdab.osrsplugin.runelite;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class GimBossUniquesPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(GimBossUniquesPlugin.class);
		RuneLite.main(args);
	}
}
