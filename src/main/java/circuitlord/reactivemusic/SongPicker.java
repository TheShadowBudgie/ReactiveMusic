package circuitlord.reactivemusic;


import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.CreditsScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;

import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.MinecartEntity;


import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;

import net.minecraft.world.World;
//import net.minecraft.world.biome.BiomeKeys;

import java.util.*;

public final class SongPicker {


	public static Map<SongpackEventType, Boolean> eventMap = new EnumMap<>(SongpackEventType.class);


	public static final Random rand = new Random();

	public static List<String> recentlyPickedSongs = new ArrayList<>();



	public static void tickEventMap() {


		MinecraftClient mc = MinecraftClient.getInstance();
		ClientPlayerEntity player = mc.player;
		World world = mc.world;


		eventMap.put(SongpackEventType.MAIN_MENU, player == null || world == null);
		eventMap.put(SongpackEventType.CREDITS, mc.currentScreen instanceof CreditsScreen);

		// Early out if not in-game
		if (player == null || world == null) return;

		// World processing
		BlockPos pos = new BlockPos(player.getBlockPos());
		var biome = world.getBiome(pos);

		boolean underground = !world.isSkyVisible(pos);
		var indimension = world.getRegistryKey();

		Entity riding = VersionHelper.GetRidingEntity(player);

		long time = world.getTimeOfDay() % 24000;
		boolean night = time > 13300 && time < 23200;
		boolean sunset = time > 12000 && time < 13000;
		boolean sunrise = time > 23000;

		// Time
		eventMap.put(SongpackEventType.DAY, !night);
		eventMap.put(SongpackEventType.NIGHT, night);
		eventMap.put(SongpackEventType.SUNSET, sunset);
		eventMap.put(SongpackEventType.SUNRISE, sunrise);


		// Actions

		eventMap.put(SongpackEventType.DYING, player.getHealth() < 7);
		eventMap.put(SongpackEventType.FISHING, player.fishHook != null);

		eventMap.put(SongpackEventType.MINECART, riding instanceof MinecartEntity);
		eventMap.put(SongpackEventType.BOAT, riding instanceof BoatEntity);
		eventMap.put(SongpackEventType.HORSE, riding instanceof HorseEntity);
		eventMap.put(SongpackEventType.PIG, riding instanceof PigEntity);


		eventMap.put(SongpackEventType.OVERWORLD, indimension == World.OVERWORLD);
		eventMap.put(SongpackEventType.NETHER, indimension == World.NETHER);
		eventMap.put(SongpackEventType.END, indimension == World.END);




		eventMap.put(SongpackEventType.UNDERGROUND, indimension == World.OVERWORLD && underground && pos.getY() < 55);
		eventMap.put(SongpackEventType.DEEP_UNDERGROUND, indimension == World.OVERWORLD && underground && pos.getY() < 15);
		eventMap.put(SongpackEventType.HIGH_UP, indimension == World.OVERWORLD && !underground && pos.getY() > 128);

		eventMap.put(SongpackEventType.UNDERWATER, player.isSubmergedInWater());



		// Weather
		eventMap.put(SongpackEventType.RAIN, world.isRaining());
		eventMap.put(SongpackEventType.SNOW, world.isRaining() && biome.value().isCold(pos));







		eventMap.put(SongpackEventType.MOUNTAIN, biome.isIn(ConventionalBiomeTags.IS_MOUNTAIN));
		eventMap.put(SongpackEventType.FOREST, biome.isIn(ConventionalBiomeTags.IS_FOREST));
		eventMap.put(SongpackEventType.BEACH, biome.isIn(ConventionalBiomeTags.IS_BEACH));
		eventMap.put(SongpackEventType.DESERT, biome.isIn(ConventionalBiomeTags.IS_DESERT));
		eventMap.put(SongpackEventType.JUNGLE, biome.isIn(ConventionalBiomeTags.IS_JUNGLE));
		eventMap.put(SongpackEventType.SAVANNA, biome.isIn(ConventionalBiomeTags.IS_SAVANNA));
		eventMap.put(SongpackEventType.OCEAN, biome.isIn(ConventionalBiomeTags.IS_OCEAN));
		// TODO:

		//eventMap.put(SongpackEventType.HOME, false);

		eventMap.put(SongpackEventType.GENERIC, true);
	}



	public static void initialize() {

		eventMap.clear();

		for (SongpackEventType eventType : SongpackEventType.values()) {
			eventMap.put(eventType, false);
		}

	}



	public static SongpackEntry getCurrentEntry() {


		for (int i = 0; i < SongLoader.activeSongpack.entries.length; i++) {

			SongpackEntry entry = SongLoader.activeSongpack.entries[i];
			if (entry == null) continue;

			boolean eventsMet = true;

			for (SongpackEventType event : entry.events) {

				if (!eventMap.containsKey(event)) continue;

				if (eventMap.get(event) == false) {
					eventsMet = false;
					break;
				}
			}

			if (eventsMet) {
				return entry;
			}

		}

		// Failed
		return null;
	}






	static String pickRandomSong(String[] songArr) {

		List<String> songs = new ArrayList<>(Arrays.stream(songArr).toList());
		songs.removeAll(recentlyPickedSongs);


		String picked;

		// If there's remaining songs, pick one of those
		if (!songs.isEmpty()) {
			int randomIndex = rand.nextInt(songs.size());
			picked = songs.get(randomIndex);
		}

		// Else we've played all these recently so just pick a new random one
		else {
			int randomIndex = rand.nextInt(songArr.length);
			picked = songArr[randomIndex];
		}


		// only track the past X songs
		if (recentlyPickedSongs.size() > 5) {
			recentlyPickedSongs.remove(0);
		}

		recentlyPickedSongs.add(picked);


		return picked;
	}


	public static String getSongName(String song) {
		return song == null ? "" : song.replaceAll("([^A-Z])([A-Z])", "$1 $2");
	}
}
