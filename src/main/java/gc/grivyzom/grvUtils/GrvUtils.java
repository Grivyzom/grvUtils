package gc.grivyzom.grvUtils;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import gc.grivyzom.grvUtils.redis.RedisManager;
import gc.grivyzom.grvUtils.redis.RedisMessenger;
import gc.grivyzom.grvUtils.redis.RedisCache;
import gc.grivyzom.grvUtils.config.ConfigManager;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
        id = "grvutils",
        name = "grvUtils",
        version = "0.1-SNAPSHOT",
        description = "Utilidades avanzadas para Velocity con soporte Redis",
        authors = {"GriVyZom"}
)
public class GrvUtils {

    private static GrvUtils instance;

    @Inject
    private Logger logger;

    @Inject
    private ProxyServer server;

    @Inject
    @DataDirectory
    private Path dataDirectory;

    private ConfigManager configManager;
    private RedisManager redisManager;
    private RedisMessenger redisMessenger;
    private RedisCache redisCache;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        instance = this;

        // Banner de inicio
        printStartupBanner();

        try {
            // Inicializar configuración
            configManager = new ConfigManager(this);
            configManager.loadConfig();

            // Inicializar Redis
            redisManager = new RedisManager(this);
            redisManager.initialize();

            // Inicializar sistemas Redis avanzados
            if (redisManager.isConnected()) {
                redisMessenger = new RedisMessenger(this);
                redisCache = new RedisCache(this);

                // Registrar algunos handlers de ejemplo
                registerExampleHandlers();

                logger.info("§a✓ §fSistemas Redis avanzados inicializados");
            }

            logger.info("§a✓ §fPlugin iniciado correctamente");
            logger.info("§b▶ §fVersión: §e0.1-SNAPSHOT");
            logger.info("§b▶ §fAutor: §eGriVyZom");

        } catch (Exception e) {
            logger.error("§c✗ §fError al inicializar el plugin:", e);
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        printShutdownBanner();

        try {
            // Cerrar conexiones Redis
            if (redisManager != null) {
                redisManager.shutdown();
                logger.info("§a✓ §fConexiones Redis cerradas correctamente");
            }

            logger.info("§a✓ §fPlugin deshabilitado correctamente");

        } catch (Exception e) {
            logger.error("§c✗ §fError al deshabilitar el plugin:", e);
        }
    }

    private void printStartupBanner() {
        logger.info("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        logger.info("§b");
        logger.info("§b    ██████╗ ██████╗ ██╗   ██╗██╗   ██╗████████╗██╗██╗     ███████╗");
        logger.info("§b   ██╔════╝ ██╔══██╗██║   ██║██║   ██║╚══██╔══╝██║██║     ██╔════╝");
        logger.info("§b   ██║  ███╗██████╔╝██║   ██║██║   ██║   ██║   ██║██║     ███████╗");
        logger.info("§b   ██║   ██║██╔══██╗╚██╗ ██╔╝██║   ██║   ██║   ██║██║     ╚════██║");
        logger.info("§b   ╚██████╔╝██║  ██║ ╚████╔╝ ╚██████╔╝   ██║   ██║███████╗███████║");
        logger.info("§b    ╚═════╝ ╚═╝  ╚═╝  ╚═══╝   ╚═════╝    ╚═╝   ╚═╝╚══════╝╚══════╝");
        logger.info("§b");
        logger.info("§f                    §eUtilidades Avanzadas para Velocity");
        logger.info("§f                         §7con soporte Redis");
        logger.info("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        logger.info("§a⚡ §fIniciando plugin...");
    }

    private void printShutdownBanner() {
        logger.info("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        logger.info("§c⏹ §fDeshabilitando §bgrvUtils§f...");
        logger.info("§7   Gracias por usar nuestro plugin!");
        logger.info("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }

    private void registerExampleHandlers() {
        // Handler para mensajes de jugadores
        redisMessenger.registerHandler("player_message", message -> {
            String playerName = message.getDataAsString("player");
            String content = message.getContent();
            logger.info("§7[CrossServer] §e" + playerName + "§f: " + content);
        });

        // Handler para eventos del servidor
        redisMessenger.registerHandler("server_event", message -> {
            String event = message.getDataAsString("event");
            String serverName = message.getDataAsString("server");
            logger.info("§7[Evento] §b" + serverName + "§f: " + event);
        });

        // Handler para sincronización de datos
        redisMessenger.registerHandler("sync_data", message -> {
            String dataType = message.getDataAsString("type");
            logger.info("§7[Sync] §aSincronizando datos: " + dataType);
        });
    }

    // Getters
    public static GrvUtils getInstance() {
        return instance;
    }

    public Logger getLogger() {
        return logger;
    }

    public ProxyServer getServer() {
        return server;
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public RedisManager getRedisManager() {
        return redisManager;
    }

    public RedisMessenger getRedisMessenger() {
        return redisMessenger;
    }

    public RedisCache getRedisCache() {
        return redisCache;
    }
}