package gc.grivyzom.grvUtils.redis;

import gc.grivyzom.grvUtils.GrvUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class RedisManager {

    private final GrvUtils plugin;
    private JedisPool jedisPool;
    private ScheduledExecutorService executorService;
    private boolean connected = false;

    public RedisManager(GrvUtils plugin) {
        this.plugin = plugin;
        this.executorService = Executors.newScheduledThreadPool(2);
    }

    public void initialize() {
        if (!plugin.getConfigManager().isRedisEnabled()) {
            plugin.getLogger().info("§7⚠ §fRedis está deshabilitado en la configuración");
            return;
        }

        try {
            // Configurar pool de conexiones
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(plugin.getConfigManager().getRedisPoolMaxTotal());
            poolConfig.setMaxIdle(plugin.getConfigManager().getRedisPoolMaxIdle());
            poolConfig.setMinIdle(plugin.getConfigManager().getRedisPoolMinIdle());
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(true);
            poolConfig.setTestWhileIdle(true);
            poolConfig.setMinEvictableIdleTimeMillis(Duration.ofSeconds(60).toMillis());
            poolConfig.setTimeBetweenEvictionRunsMillis(Duration.ofSeconds(30).toMillis());
            poolConfig.setNumTestsPerEvictionRun(3);
            poolConfig.setBlockWhenExhausted(true);

            // Crear pool
            String password = plugin.getConfigManager().getRedisPassword();
            if (password.isEmpty()) {
                jedisPool = new JedisPool(
                        poolConfig,
                        plugin.getConfigManager().getRedisHost(),
                        plugin.getConfigManager().getRedisPort(),
                        plugin.getConfigManager().getRedisTimeout(),
                        null,
                        plugin.getConfigManager().getRedisDatabase()
                );
            } else {
                jedisPool = new JedisPool(
                        poolConfig,
                        plugin.getConfigManager().getRedisHost(),
                        plugin.getConfigManager().getRedisPort(),
                        plugin.getConfigManager().getRedisTimeout(),
                        password,
                        plugin.getConfigManager().getRedisDatabase()
                );
            }

            // Probar conexión
            testConnection();
            connected = true;

            plugin.getLogger().info("§a✓ §fRedis conectado correctamente");
            plugin.getLogger().info("§b▶ §fHost: §e" + plugin.getConfigManager().getRedisHost() + ":" + plugin.getConfigManager().getRedisPort());
            plugin.getLogger().info("§b▶ §fBase de datos: §e" + plugin.getConfigManager().getRedisDatabase());

        } catch (Exception e) {
            plugin.getLogger().error("§c✗ §fError al conectar con Redis:", e);
            connected = false;
        }
    }

    private void testConnection() throws Exception {
        try (Jedis jedis = jedisPool.getResource()) {
            String response = jedis.ping();
            if (!"PONG".equals(response)) {
                throw new Exception("Respuesta inesperada del ping: " + response);
            }
        }
    }

    public void shutdown() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }

        connected = false;
    }

    public boolean isConnected() {
        return connected && jedisPool != null && !jedisPool.isClosed();
    }

    // Métodos síncronos
    public void set(String key, String value) {
        if (!isConnected()) return;

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set(key, value);
        } catch (Exception e) {
            plugin.getLogger().error("Error al establecer valor en Redis:", e);
        }
    }

    public void set(String key, String value, int seconds) {
        if (!isConnected()) return;

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(key, seconds, value);
        } catch (Exception e) {
            plugin.getLogger().error("Error al establecer valor con expiración en Redis:", e);
        }
    }

    public String get(String key) {
        if (!isConnected()) return null;

        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get(key);
        } catch (Exception e) {
            plugin.getLogger().error("Error al obtener valor de Redis:", e);
            return null;
        }
    }

    public void delete(String key) {
        if (!isConnected()) return;

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key);
        } catch (Exception e) {
            plugin.getLogger().error("Error al eliminar clave de Redis:", e);
        }
    }

    public boolean exists(String key) {
        if (!isConnected()) return false;

        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.exists(key);
        } catch (Exception e) {
            plugin.getLogger().error("Error al verificar existencia de clave en Redis:", e);
            return false;
        }
    }

    public void expire(String key, int seconds) {
        if (!isConnected()) return;

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.expire(key, seconds);
        } catch (Exception e) {
            plugin.getLogger().error("Error al establecer expiración en Redis:", e);
        }
    }

    // Métodos asíncronos
    public CompletableFuture<Void> setAsync(String key, String value) {
        return CompletableFuture.runAsync(() -> set(key, value), executorService);
    }

    public CompletableFuture<Void> setAsync(String key, String value, int seconds) {
        return CompletableFuture.runAsync(() -> set(key, value, seconds), executorService);
    }

    public CompletableFuture<String> getAsync(String key) {
        return CompletableFuture.supplyAsync(() -> get(key), executorService);
    }

    public CompletableFuture<Void> deleteAsync(String key) {
        return CompletableFuture.runAsync(() -> delete(key), executorService);
    }

    public CompletableFuture<Boolean> existsAsync(String key) {
        return CompletableFuture.supplyAsync(() -> exists(key), executorService);
    }

    // Pub/Sub
    public void publish(String channel, String message) {
        if (!isConnected()) return;

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.publish(channel, message);
        } catch (Exception e) {
            plugin.getLogger().error("Error al publicar mensaje en Redis:", e);
        }
    }

    public CompletableFuture<Void> publishAsync(String channel, String message) {
        return CompletableFuture.runAsync(() -> publish(channel, message), executorService);
    }

    public void subscribe(JedisPubSub pubSub, String... channels) {
        if (!isConnected()) return;

        executorService.execute(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.subscribe(pubSub, channels);
            } catch (Exception e) {
                plugin.getLogger().error("Error al suscribirse a canal de Redis:", e);
            }
        });
    }

    // Hash operations
    public void hset(String key, String field, String value) {
        if (!isConnected()) return;

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hset(key, field, value);
        } catch (Exception e) {
            plugin.getLogger().error("Error al establecer valor hash en Redis:", e);
        }
    }

    public String hget(String key, String field) {
        if (!isConnected()) return null;

        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.hget(key, field);
        } catch (Exception e) {
            plugin.getLogger().error("Error al obtener valor hash de Redis:", e);
            return null;
        }
    }

    public void hdel(String key, String... fields) {
        if (!isConnected()) return;

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hdel(key, fields);
        } catch (Exception e) {
            plugin.getLogger().error("Error al eliminar campo hash de Redis:", e);
        }
    }

    // Getters
    public JedisPool getJedisPool() {
        return jedisPool;
    }

    public ScheduledExecutorService getExecutorService() {
        return executorService;
    }
}