package circuitlord.reactivemusic;

import java.io.File;

public class SongConditions {
    public static final String[] specialEvents = {
            "MAIN_MENU",
            "UNDERGROUND",
            "DEEP_UNDERGROUND",
            "HIGH_UP",
            "MINECART",
            "UNDERWATER",
            "BOAT",
            "HORSE",
            "PIG",
            "FISHING",
            "DYING",
            "END",
            "NETHER",
            "SUNSET",
            "SUNRISE",
            "CREDITS"
    };
    static File getDaySongs(String event){
        String stringSongPath;
            stringSongPath = String.valueOf(SongLoader.activeSongpackPath.resolve("music").resolve("BIOMES").resolve(event).resolve("day"));
        return new File(stringSongPath);
    }
    static File getNightSongs(String event){
        String stringSongPath;
        stringSongPath = String.valueOf(SongLoader.activeSongpackPath.resolve("music").resolve(event).resolve("BIOMES").resolve("night"));
        return new File(stringSongPath);
    }
    static File getRainSongs(String event){
        String stringSongPath;
        if(SongPicker.checkNight) {
            stringSongPath = String.valueOf(SongLoader.activeSongpackPath.resolve("music").resolve("BIOMES").resolve(event).resolve("night").resolve("rain"));
        } else {
            stringSongPath = String.valueOf(SongLoader.activeSongpackPath.resolve("music").resolve("BIOMES").resolve(event).resolve("day").resolve("rain"));
        }
        return new File(stringSongPath);
    }
    static File getSpecialSongs(String event){
        String stringSongPath;
        stringSongPath = String.valueOf(SongLoader.activeSongpackPath.resolve("music").resolve("SPECIAL_EVENTS").resolve(event));
        return new File(stringSongPath);
    }
    static File getGenericSongs(Boolean night, Boolean rain){
        String stringSongPath;
        try {
            if (rain && night){
                stringSongPath = String.valueOf(SongLoader.activeSongpackPath.resolve("music").resolve("night").resolve("rain"));
            } else if (!night && rain) {
                stringSongPath = String.valueOf(SongLoader.activeSongpackPath.resolve("music").resolve("day").resolve("rain"));
            } else if (night) {
                stringSongPath = String.valueOf(SongLoader.activeSongpackPath.resolve("music").resolve("night"));
            } else {
                stringSongPath = String.valueOf(SongLoader.activeSongpackPath.resolve("music").resolve("day"));
            }
            return new File(stringSongPath);
        } catch (Exception e){
            return null;
        }
    }
}
