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
import org.yaml.snakeyaml.util.ArrayUtils;
//import net.minecraft.world.biome.BiomeKeys;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public final class SongPicker {
	public static boolean stringContainsItemFromList(String inputStr, String[] items) {
		return Arrays.stream(items).anyMatch(inputStr::contains);
	}

	public static Map<SongpackEventType, Boolean> eventMap = new EnumMap<>(SongpackEventType.class);
	public static Boolean checkRaining = new Boolean(null);
	public static Boolean checkNight = new Boolean(null);
	public static final Random rand = new Random();
	public static List<String> recentlyPickedSongs = new ArrayList<>();
	public static void setNight(Boolean var){
		SongPicker.checkNight = var;
	}
	public static void setWeather(Boolean var){
		SongPicker.checkRaining = var;
	}

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

		//Time of day
		long time = world.getTimeOfDay() % 24000;
		boolean isNight = time > 13300 && time < 23200;
		boolean sunset = time > 12000 && time < 13000;
		boolean sunrise = time > 23000;

		setNight(isNight);
		//Weather
		boolean isRaining = world.isRaining();
		setWeather(isRaining);

		// Time
		/*eventMap.put(SongpackEventType.DAY, !isNight);
		eventMap.put(SongpackEventType.NIGHT, isNight);*/
		eventMap.put(SongpackEventType.SUNSET, sunset);
		eventMap.put(SongpackEventType.SUNRISE, sunrise);


		// Actions
		eventMap.put(SongpackEventType.DYING, player.getHealth() < 7);
		eventMap.put(SongpackEventType.FISHING, player.fishHook != null);
		eventMap.put(SongpackEventType.MINECART, riding instanceof MinecartEntity);
		eventMap.put(SongpackEventType.BOAT, riding instanceof BoatEntity);
		eventMap.put(SongpackEventType.HORSE, riding instanceof HorseEntity);
		eventMap.put(SongpackEventType.PIG, riding instanceof PigEntity);

		//Dimensions
		//eventMap.put(SongpackEventType.OVERWORLD, indimension == World.OVERWORLD);
		eventMap.put(SongpackEventType.NETHER, indimension == World.NETHER);
		eventMap.put(SongpackEventType.END, indimension == World.END);

		//World Height
		eventMap.put(SongpackEventType.UNDERGROUND, indimension == World.OVERWORLD && underground && pos.getY() < 55);
		eventMap.put(SongpackEventType.DEEP_UNDERGROUND, indimension == World.OVERWORLD && underground && pos.getY() < 15);
		eventMap.put(SongpackEventType.HIGH_UP, indimension == World.OVERWORLD && !underground && pos.getY() > 128);

		//Underwater
		eventMap.put(SongpackEventType.UNDERWATER, player.isSubmergedInWater());

		//Biomes
		eventMap.put(SongpackEventType.MOUNTAIN, biome.isIn(ConventionalBiomeTags.IS_MOUNTAIN));
		eventMap.put(SongpackEventType.FOREST, biome.isIn(ConventionalBiomeTags.IS_FOREST));
		eventMap.put(SongpackEventType.BEACH, biome.isIn(ConventionalBiomeTags.IS_BEACH));
		eventMap.put(SongpackEventType.DESERT, biome.isIn(ConventionalBiomeTags.IS_DESERT));
		eventMap.put(SongpackEventType.JUNGLE, biome.isIn(ConventionalBiomeTags.IS_JUNGLE));
		eventMap.put(SongpackEventType.SAVANNA, biome.isIn(ConventionalBiomeTags.IS_SAVANNA));
		eventMap.put(SongpackEventType.OCEAN, biome.isIn(ConventionalBiomeTags.IS_OCEAN));

		//If all else Fails
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
				if (!eventMap.get(event)) {
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

	public static String[] getSongs(Boolean isRaining, Boolean isNight){
		//Variable Declaration and initialization
		String stringsongPath;
		String[] songList;
		String event = String.valueOf(getCurrentEntry().events[0]);
		if (stringContainsItemFromList(event, SongConditions.specialEvents)){
			if (SongLoader.activeSongpackPath == null) {
				stringsongPath = "/musicpack/music/"+event+"/DoNotRemove.mp3";
				File folder = new File(stringsongPath);
				ReactiveMusic.LOGGER.error(stringsongPath);
				return songList = new String[]{stringsongPath.toString()};
				//return songList;
			} else {
				String[] songsInFolder = getSongsInFolder(new File(String.valueOf(SongConditions.getSpecialSongs(event))));
				if(songsInFolder != null){
					return songsInFolder;
				}else {
					return getSongsInFolder(new File(String.valueOf(SongConditions.getGenericSongs(isNight, isRaining))));
				}
			}

		}
		if(isRaining) {
			String[] songsInFolder = getSongsInFolder(new File(String.valueOf(SongConditions.getRainSongs(event))));
			if(songsInFolder != null){
				return songsInFolder;
			}else {
				return getSongsInFolder(new File(String.valueOf(SongConditions.getGenericSongs(isNight, isRaining))));
			}
		} else if (isNight) {
			String[] songsInFolder = getSongsInFolder(new File(String.valueOf(SongConditions.getNightSongs(event))));
			if(songsInFolder != null){
				return songsInFolder;
			}else {
				return getSongsInFolder(new File(String.valueOf(SongConditions.getGenericSongs(isNight, isRaining))));
			}
		} else {
			String[] songsInFolder = getSongsInFolder(new File(String.valueOf(SongConditions.getDaySongs(event))));
			if(songsInFolder != null){
				return songsInFolder;
			}else {
				return getSongsInFolder(new File(String.valueOf(SongConditions.getGenericSongs(isNight, isRaining))));
			}
		}
	}

	public static String[] getSongsInFolder(File filesInFolder){
		try{
			String[] songList;
			File[] files = filesInFolder.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					String name = pathname.getName().toLowerCase();
					return pathname.isFile() && name.endsWith(".mp3");
				}
		});
		songList = new String[files.length];//Arrays.copyOf(files, files.length,  );
			if (songList != null && songList.length > 0){
				for (int i = 0; i < files.length; i++){
					songList[i] = files[i].getPath();
				}
				return songList;}
		}catch (NullPointerException e){
			ReactiveMusic.LOGGER.error(e.toString());
		}
        return null;
    }

	static String pickRandomSong(String[] songArr) {
		String picked;
		List<String> songs = new ArrayList<>(Arrays.stream(songArr).toList());
		songs.removeAll(recentlyPickedSongs);
		// If there's remaining songs, pick one of those
		if (!songs.isEmpty()) {
			int randomIndex = rand.nextInt(songs.size());
			picked = songs.get(randomIndex);
		} else {
			int randomIndex = rand.nextInt(songArr.length);
			picked = songArr[randomIndex];
		}
		// only track the past X songs
		if (recentlyPickedSongs.size() > 5) {
			recentlyPickedSongs.removeFirst();
		}
		recentlyPickedSongs.add(picked);
		return picked;
	}

	public static String getSongName(String song) {
		return song == null ? "" : song.replaceAll("([^A-Z])([A-Z])", "$1 $2");
	}
}
