package gc.grivyzom.grvUtils.config;

import gc.grivyzom.grvUtils.GrvUtils;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {

    private final GrvUtils plugin;
    private final Path configFile;
    private CommentedConfigurationNode config;

    public ConfigManager(GrvUtils plugin) {
        this.plugin = plugin;
        this.configFile = plugin.getDataDirectory().resolve("config.yml");
    }

    public void loadConfig() throws IOException {
        // Crear directorio si no existe
        if (!Files.exists(plugin.getDataDirectory())) {
            Files.createDirectories(plugin.getDataDirectory());
        }

        // Crear archivo de configuración por defecto si no existe
        if (!Files.exists(configFile)) {
            createDefaultConfig();
        }

        // Cargar configuración
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(configFile)
                .build();

        config = loader.load();
        plugin.getLogger().info("§a✓ §fConfiguración cargada correctamente");
    }

    private void createDefaultConfig() throws IOException {
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(configFile)
                .build();

        CommentedConfigurationNode root = loader.createNode();

        // Configuración Redis
        root.node("redis", "enabled").set(true)
                .comment("Habilitar/deshabilitar Redis");
        root.node("redis", "host").set("localhost")
                .comment("Host del servidor Redis");
        root.node("redis", "port").set(6379)
                .comment("Puerto del servidor Redis");
        root.node("redis", "password").set("")
                .comment("Contraseña de Redis (dejar vacío si no tiene)");
        root.node("redis", "database").set(0)
                .comment("Base de datos Redis (0-15)");
        root.node("redis", "timeout").set(2000)
                .comment("Timeout de conexión en milisegundos");
        root.node("redis", "pool", "max-total").set(20)
                .comment("Máximo número de conexiones en el pool");
        root.node("redis", "pool", "max-idle").set(10)
                .comment("Máximo número de conexiones inactivas");
        root.node("redis", "pool", "min-idle").set(2)
                .comment("Mínimo número de conexiones inactivas");

        // Configuración general
        root.node("general", "debug").set(false)
                .comment("Habilitar modo debug");
        root.node("general", "language").set("es")
                .comment("Idioma del plugin (es/en)");

        loader.save(root);
        plugin.getLogger().info("§a✓ §fArchivo de configuración creado");
    }

    public void saveConfig() throws IOException {
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(configFile)
                .build();
        loader.save(config);
    }

    // Métodos para obtener valores de configuración
    public boolean isRedisEnabled() {
        return config.node("redis", "enabled").getBoolean(true);
    }

    public String getRedisHost() {
        return config.node("redis", "host").getString("localhost");
    }

    public int getRedisPort() {
        return config.node("redis", "port").getInt(6379);
    }

    public String getRedisPassword() {
        return config.node("redis", "password").getString("");
    }

    public int getRedisDatabase() {
        return config.node("redis", "database").getInt(0);
    }

    public int getRedisTimeout() {
        return config.node("redis", "timeout").getInt(2000);
    }

    public int getRedisPoolMaxTotal() {
        return config.node("redis", "pool", "max-total").getInt(20);
    }

    public int getRedisPoolMaxIdle() {
        return config.node("redis", "pool", "max-idle").getInt(10);
    }

    public int getRedisPoolMinIdle() {
        return config.node("redis", "pool", "min-idle").getInt(2);
    }

    public boolean isDebugEnabled() {
        return config.node("general", "debug").getBoolean(false);
    }

    public String getLanguage() {
        return config.node("general", "language").getString("es");
    }

    public CommentedConfigurationNode getConfig() {
        return config;
    }
}