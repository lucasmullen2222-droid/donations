package donations.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("donoalerts.json");

    public String displayName = "Uh_dylan";
    /** Payments below this are ignored (1,000,000 = 1M). */
    public double minAmount = 1_000_000;
    public boolean soundsEnabled = true;
    public boolean screenShake = true;

    public static Config load() {
        try {
            if (Files.exists(FILE)) {
                Config c = GSON.fromJson(Files.readString(FILE), Config.class);
                if (c != null) return c;
            }
        } catch (Exception e) {
            DonoAlertsClient.LOGGER.error("Could not read config", e);
        }
        Config c = new Config();
        c.save();
        return c;
    }

    public void save() {
        try { Files.writeString(FILE, GSON.toJson(this)); }
        catch (Exception e) { DonoAlertsClient.LOGGER.error("Could not save config", e); }
    }
}
