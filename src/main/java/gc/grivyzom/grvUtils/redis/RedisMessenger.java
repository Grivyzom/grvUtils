package gc.grivyzom.grvUtils.redis;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import gc.grivyzom.grvUtils.GrvUtils;
import redis.clients.jedis.JedisPubSub;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Sistema de mensajería avanzado usando Redis Pub/Sub
 */
public class RedisMessenger {

    private final GrvUtils plugin;
    private final RedisManager redisManager;
    private final Gson gson;
    private final Map<String, Consumer<RedisMessage>> messageHandlers;
    private final String serverIdentifier;

    public RedisMessenger(GrvUtils plugin) {
        this.plugin = plugin;
        this.redisManager = plugin.getRedisManager();
        this.gson = new Gson();
        this.messageHandlers = new ConcurrentHashMap<>();
        this.serverIdentifier = generateServerIdentifier();

        // Suscribirse al canal principal
        subscribeToMainChannel();
    }

    private String generateServerIdentifier() {
        return "velocity-" + System.currentTimeMillis() + "-" +
                Integer.toHexString(hashCode());
    }

    private void subscribeToMainChannel() {
        redisManager.subscribe(new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                handleIncomingMessage(channel, message);
            }

            @Override
            public void onSubscribe(String channel, int subscribedChannels) {
                plugin.getLogger().info("§a✓ §fSuscrito al canal Redis: §e" + channel);
            }

            @Override
            public void onUnsubscribe(String channel, int subscribedChannels) {
                plugin.getLogger().info("§7- §fDesuscrito del canal Redis: §e" + channel);
            }
        }, "grvutils:main", "grvutils:broadcast");
    }

    private void handleIncomingMessage(String channel, String rawMessage) {
        try {
            RedisMessage message = gson.fromJson(rawMessage, RedisMessage.class);

            // Ignorar mensajes de este mismo servidor
            if (serverIdentifier.equals(message.getSender())) {
                return;
            }

            // Buscar handler para el tipo de mensaje
            Consumer<RedisMessage> handler = messageHandlers.get(message.getType());
            if (handler != null) {
                handler.accept(message);
            }

            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("§7[DEBUG] §fMensaje Redis recibido: §e" +
                        message.getType() + " §7de §e" + message.getSender());
            }

        } catch (JsonSyntaxException e) {
            plugin.getLogger().error("Error al deserializar mensaje Redis:", e);
        }
    }

    /**
     * Registra un handler para un tipo específico de mensaje
     */
    public void registerHandler(String messageType, Consumer<RedisMessage> handler) {
        messageHandlers.put(messageType, handler);
        plugin.getLogger().info("§a✓ §fHandler registrado para tipo: §e" + messageType);
    }

    /**
     * Envía un mensaje a todos los servidores
     */
    public void sendMessage(String type, String content) {
        sendMessage(type, content, null);
    }

    /**
     * Envía un mensaje con datos adicionales
     */
    public void sendMessage(String type, String content, Map<String, Object> data) {
        RedisMessage message = new RedisMessage(
                type,
                content,
                serverIdentifier,
                System.currentTimeMillis(),
                data
        );

        String json = gson.toJson(message);
        redisManager.publishAsync("grvutils:main", json)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        plugin.getLogger().error("Error al enviar mensaje Redis:", throwable);
                    } else if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("§7[DEBUG] §fMensaje Redis enviado: §e" + type);
                    }
                });
    }

    /**
     * Envía un mensaje de broadcast (a todos los servidores)
     */
    public void broadcast(String type, String content) {
        broadcast(type, content, null);
    }

    /**
     * Envía un mensaje de broadcast con datos adicionales
     */
    public void broadcast(String type, String content, Map<String, Object> data) {
        RedisMessage message = new RedisMessage(
                type,
                content,
                serverIdentifier,
                System.currentTimeMillis(),
                data
        );

        String json = gson.toJson(message);
        redisManager.publishAsync("grvutils:broadcast", json);
    }

    /**
     * Clase que representa un mensaje Redis
     */
    public static class RedisMessage {
        private final String type;
        private final String content;
        private final String sender;
        private final long timestamp;
        private final Map<String, Object> data;

        public RedisMessage(String type, String content, String sender,
                            long timestamp, Map<String, Object> data) {
            this.type = type;
            this.content = content;
            this.sender = sender;
            this.timestamp = timestamp;
            this.data = data;
        }

        public String getType() { return type; }
        public String getContent() { return content; }
        public String getSender() { return sender; }
        public long getTimestamp() { return timestamp; }
        public Map<String, Object> getData() { return data; }

        public <T> T getData(String key, Class<T> type) {
            if (data == null) return null;
            Object value = data.get(key);
            return type.isInstance(value) ? type.cast(value) : null;
        }

        public String getDataAsString(String key) {
            return getData(key, String.class);
        }

        public Integer getDataAsInt(String key) {
            Object value = data != null ? data.get(key) : null;
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return null;
        }

        public Boolean getDataAsBoolean(String key) {
            return getData(key, Boolean.class);
        }
    }

    public String getServerIdentifier() {
        return serverIdentifier;
    }
}