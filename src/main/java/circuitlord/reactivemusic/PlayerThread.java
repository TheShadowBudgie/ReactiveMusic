package circuitlord.reactivemusic;


import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.sound.SoundCategory;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.JavaSoundAudioDevice;
import javazoom.jl.player.advanced.AdvancedPlayer;
import net.minecraft.text.TranslatableTextContent;

import java.io.IOException;
import java.io.InputStream;

public class PlayerThread extends Thread {

	public static final float MIN_POSSIBLE_GAIN = -80F;
	public static final float MIN_GAIN = -50F;
	public static final float MAX_GAIN = 5F;

	public static float[] fadeGains;
	
	static {
		fadeGains = new float[ReactiveMusic.FADE_DURATION];
		float totaldiff = MIN_GAIN - MAX_GAIN;
		float diff = totaldiff / fadeGains.length;
		for(int i = 0; i < fadeGains.length; i++)
			fadeGains[i] = MAX_GAIN + diff * i;

		// Invert because we have fade ticks counting up now
		//for (int i = fadeGains.length - 1; i >= 0; i--) {
		//	fadeGains[i] = MAX_GAIN + diff * (fadeGains.length - 1 - i);
		//}
	}
	
	public volatile static float gainPercentage = 1.0f;

	public static final float QUIET_VOLUME_PERCENTAGE = 0.7f;
	public static final float QUIET_VOLUME_LERP_RATE = 0.015f;
	public static float quietPercentage = 1.0f;

	public volatile static float realGain = 0;

	public volatile static String currentSong = null;
	public volatile static String currentSongChoices = null;

	public volatile SongResource currentSongResource = null;
	
	AdvancedPlayer player;

	private volatile boolean queued = false;

	private volatile boolean kill = false;
	private volatile boolean playing = false;


	boolean notQueuedOrPlaying() {
		return !(queued || isPlaying());
	}

	boolean isPlaying() {
		Boolean test;
		test =  playing && !player.getComplete();
		return test;
	}
	
	public PlayerThread() {
		setDaemon(true);
		setName("ReactiveMusic Player Thread");
		start();
	}

	@Override
	public void run() {
		try {
			while(!kill) {

				if(queued && currentSong != null) {

					currentSongResource = SongLoader.getStream(currentSong);
					if(currentSongResource == null || currentSongResource.inputStream == null)
						continue;

					player = new AdvancedPlayer(currentSongResource.inputStream);
					queued = false;

				}
				if(player != null && player.getAudioDevice() != null) {

					// go to full volume
					setGainPercentage(1.0f);
					processRealGain();

					ReactiveMusic.LOGGER.info("Playing " + currentSong);
					playing = true;
					player.play();

				}

			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void resetPlayer() {
		playing = false;

		if(player != null)
			player.queuedToStop = true;

		currentSong = null;

		if (currentSongResource != null && currentSongResource.fileSystem != null) {
            try {
                currentSongResource.fileSystem.close();
            } catch (IOException e) {
                ReactiveMusic.LOGGER.error("Failed to close file system input stream " + e.getMessage());
            }
        }

		currentSongResource = null;
	}

	public void play(String song) {
		resetPlayer();

		currentSong = song;
        queued = true;
	}

	public void setGainPercentage(float newGain) {
		gainPercentage = Math.min(1.0f, Math.max(0.0f, newGain));
	}
	
	public void processRealGain() {

		var client = MinecraftClient.getInstance();

		GameOptions options = MinecraftClient.getInstance().options;

		boolean musicOptionsOpen = false;

		// Try to find the music options menu
		TranslatableTextContent ScreenTitleContent = null;
		if (client.currentScreen != null && client.currentScreen.getTitle() != null && client.currentScreen.getTitle().getContent() != null
			&& client.currentScreen.getTitle().getContent() instanceof TranslatableTextContent) {

			ScreenTitleContent = (TranslatableTextContent) client.currentScreen.getTitle().getContent();

			if (ScreenTitleContent != null) {
				musicOptionsOpen = ScreenTitleContent.getKey().equals("options.sounds.title");
			}
		}


		boolean doQuietMusic =  client.isPaused()
				&& client.world != null
				&& !musicOptionsOpen;


		float targetQuietMusicPercentage = doQuietMusic ? QUIET_VOLUME_PERCENTAGE : 1.0f;
        quietPercentage = MyMath.lerpConstant(quietPercentage, targetQuietMusicPercentage, QUIET_VOLUME_LERP_RATE);

		
		float minecraftGain = options.getSoundVolume(SoundCategory.MUSIC) * options.getSoundVolume(SoundCategory.MASTER);
		float newRealGain = MIN_GAIN + (MAX_GAIN - MIN_GAIN) * minecraftGain * gainPercentage * quietPercentage;

		// Force to basically off if the user sets their volume off
		if (minecraftGain <= 0) {
			newRealGain = MIN_POSSIBLE_GAIN;
		}


		
		realGain = newRealGain;
		if(player != null) {
			AudioDevice device = player.getAudioDevice();
			if(device != null && device instanceof JavaSoundAudioDevice) {
				try {
					((JavaSoundAudioDevice) device).setGain(newRealGain);
				} catch(IllegalArgumentException e) {
					ReactiveMusic.LOGGER.error(e.toString());
				}
			}
		}
	}

	public void forceKill() {
		try {
			resetPlayer();
			interrupt();

			finalize();
			kill = true;
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}
}
