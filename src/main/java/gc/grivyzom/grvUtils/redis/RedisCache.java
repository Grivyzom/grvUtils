package gc.grivyzom.grvUtils.redis;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import gc.grivyzom.grvUtils.GrvUtils;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Sistema de caché avanzado usando Redis con soporte para objetos complejos
 */
public class RedisCache {

    private final GrvUtils plugin;
    private final RedisManager redisManager;
    private final Gson gson;
    private final String keyPrefix;

    public RedisCache(GrvUtils plugin) {
        this.plugin = plugin;
        this.redisManager = plugin.getRedisManager();
        this.gson = new Gson();
        this.keyPrefix = "grvutils:cache:";
    }

    private String buildKey(String key) {
        return keyPrefix + key;
    }

    // Métodos para String
    public void set(String key, String value) {
        redisManager.set(buildKey(key), value);
    }

    public void set(String key, String value, int ttlSeconds) {
        redisManager.set(buildKey(key), value, ttlSeconds);
    }

    public String getString(String key) {
        return redisManager.get(buildKey(key));
    }

    public CompletableFuture<String> getStringAsync(String key) {
        return redisManager.getAsync(buildKey(key));
    }

    // Métodos para objetos (usando JSON)
    public <T> void setObject(String key, T object) {
        String json = gson.toJson(object);
        redisManager.set(buildKey(key), json);
    }

    public <T> void setObject(String key, T object, int ttlSeconds) {
        String json = gson.toJson(object);
        redisManager.set(buildKey(key), json, ttlSeconds);
    }

    public <T> T getObject(String key, Class<T> clazz) {
        String json = redisManager.get(buildKey(key));
        if (json == null) return null;

        try {
            return gson.fromJson(json, clazz);
        } catch (Exception e) {
            plugin.getLogger().error("Error al deserializar objeto desde Redis:", e);
            return null;
        }
    }

    public <T> T getObject(String key, Type type) {
        String json = redisManager.get(buildKey(key));
        if (json == null) return null;

        try {
            return gson.fromJson(json, type);
        } catch (Exception e) {
            plugin.getLogger().error("Error al deserializar objeto desde Redis:", e);
            return null;
        }
    }

    public <T> CompletableFuture<T> getObjectAsync(String key, Class<T> clazz) {
        return redisManager.getAsync(buildKey(key))
                .thenApply(json -> {
                    if (json == null) return null;
                    try {
                        return gson.fromJson(json, clazz);
                    } catch (Exception e) {
                        plugin.getLogger().error("Error al deserializar objeto desde Redis:", e);
                        return null;
                    }
                });
    }

    // Métodos para listas
    public <T> void setList(String key, List<T> list) {
        String json = gson.toJson(list);
        redisManager.set(buildKey(key), json);
    }

    public <T> void setList(String key, List<T> list, int ttlSeconds) {
        String json = gson.toJson(list);
        redisManager.set(buildKey(key), json, ttlSeconds);
    }

    public <T> List<T> getList(String key, Class<T> elementClass) {
        String json = redisManager.get(buildKey(key));
        if (json == null) return null;

        try {
            Type listType = TypeToken.getParameterized(List.class, elementClass).getType();
            return gson.fromJson(json, listType);
        } catch (Exception e) {
            plugin.getLogger().error("Error al deserializar lista desde Redis:", e);
            return null;
        }
    }

    // Métodos para mapas
    public <K, V> void setMap(String key, Map<K, V> map) {
        String json = gson.toJson(map);
        redisManager.set(buildKey(key), json);
    }

    public <K, V> void setMap(String key, Map<K, V> map, int ttlSeconds) {
        String json = gson.toJson(map);
        redisManager.set(buildKey(key), json, ttlSeconds);
    }

    public <K, V> Map<K, V> getMap(String key, Class<K> keyClass, Class<V> valueClass) {
        String json = redisManager.get(buildKey(key));
        if (json == null) return null;

        try {
            Type mapType = TypeToken.getParameterized(Map.class, keyClass, valueClass).getType();
            return gson.fromJson(json, mapType);
        } catch (Exception e) {
            plugin.getLogger().error("Error al deserializar mapa desde Redis:", e);
            return null;
        }
    }

    // Métodos para sets
    public <T> void setSet(String key, Set<T> set) {
        String json = gson.toJson(set);
        redisManager.set(buildKey(key), json);
    }

    public <T> void setSet(String key, Set<T> set, int ttlSeconds) {
        String json = gson.toJson(set);
        redisManager.set(buildKey(key), json, ttlSeconds);
    }

    public <T> Set<T> getSet(String key, Class<T> elementClass) {
        String json = redisManager.get(buildKey(key));
        if (json == null) return null;

        try {
            Type setType = TypeToken.getParameterized(Set.class, elementClass).getType();
            return gson.fromJson(json, setType);
        } catch (Exception e) {
            plugin.getLogger().error("Error al deserializar set desde Redis:", e);
            return null;
        }
    }

    // Métodos generales
    public boolean exists(String key) {
        return redisManager.exists(buildKey(key));
    }

    public CompletableFuture<Boolean> existsAsync(String key) {
        return redisManager.existsAsync(buildKey(key));
    }

    public void delete(String key) {
        redisManager.delete(buildKey(key));
    }

    public CompletableFuture<Void> deleteAsync(String key) {
        return redisManager.deleteAsync(buildKey(key));
    }

    public void expire(String key, int seconds) {
        redisManager.expire(buildKey(key), seconds);
    }

    // Métodos de conveniencia para tipos primitivos
    public void setInt(String key, int value) {
        redisManager.set(buildKey(key), String.valueOf(value));
    }

    public void setInt(String key, int value, int ttlSeconds) {
        redisManager.set(buildKey(key), String.valueOf(value), ttlSeconds);
    }

    public Integer getInt(String key) {
        String value = redisManager.get(buildKey(key));
        if (value == null) return null;

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void setLong(String key, long value) {
        redisManager.set(buildKey(key), String.valueOf(value));
    }

    public void setLong(String key, long value, int ttlSeconds) {
        redisManager.set(buildKey(key), String.valueOf(value), ttlSeconds);
    }

    public Long getLong(String key) {
        String value = redisManager.get(buildKey(key));
        if (value == null) return null;

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void setBoolean(String key, boolean value) {
        redisManager.set(buildKey(key), String.valueOf(value));
    }

    public void setBoolean(String key, boolean value, int ttlSeconds) {
        redisManager.set(buildKey(key), String.valueOf(value), ttlSeconds);
    }

    public Boolean getBoolean(String key) {
        String value = redisManager.get(buildKey(key));
        if (value == null) return null;

        return Boolean.parseBoolean(value);
    }

    public void setDouble(String key, double value) {
        redisManager.set(buildKey(key), String.valueOf(value));
    }

    public void setDouble(String key, double value, int ttlSeconds) {
        redisManager.set(buildKey(key), String.valueOf(value), ttlSeconds);
    }

    public Double getDouble(String key) {
        String value = redisManager.get(buildKey(key));
        if (value == null) return null;

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}