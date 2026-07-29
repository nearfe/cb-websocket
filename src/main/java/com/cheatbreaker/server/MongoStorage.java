package com.cheatbreaker.server;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB persistence for player cosmetics (emotes).
 * Stores each player's owned emote IDs so they persist across server restarts.
 *
 * Collection: "players" in database "cheatbreaker"
 * Document format: { "_id": "<username>", "emotes": [0, 1, 2, ...] }
 */
public class MongoStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoStorage.class);

    private final MongoClient client;
    private final MongoDatabase database;
    private final MongoCollection<Document> playersCollection;

    public MongoStorage(String connectionString, String dbName) {
        this.client = MongoClients.create(connectionString);
        this.database = client.getDatabase(dbName);
        this.playersCollection = database.getCollection("players");
        LOGGER.info("MongoDB connected: {} / {}", connectionString, dbName);
    }

    /**
     * Load a player's owned emote IDs from the database.
     * Returns an empty list if the player has no document yet.
     */
    public List<Integer> loadEmotes(String username) {
        Document doc = playersCollection.find(Filters.eq("_id", username.toLowerCase())).first();
        if (doc == null) {
            return new ArrayList<>();
        }
        List<Integer> emotes = new ArrayList<>();
        List<?> raw = doc.getList("emotes", Integer.class);
        if (raw != null) {
            for (Object o : raw) {
                if (o instanceof Number) {
                    emotes.add(((Number) o).intValue());
                }
            }
        }
        return emotes;
    }

    /**
     * Save (overwrite) a player's emote list.
     */
    public void saveEmotes(String username, List<Integer> emotes) {
        Document doc = new Document("_id", username.toLowerCase())
                .append("emotes", emotes);
        playersCollection.replaceOne(
                Filters.eq("_id", username.toLowerCase()),
                doc,
                new ReplaceOptions().upsert(true)
        );
    }

    /**
     * Add a single emote to a player's list (no-op if already owned).
     */
    public void addEmote(String username, int emoteId) {
        List<Integer> emotes = loadEmotes(username);
        if (!emotes.contains(emoteId)) {
            emotes.add(emoteId);
            saveEmotes(username, emotes);
        }
    }

    /**
     * Remove a single emote from a player's list.
     */
    public void removeEmote(String username, int emoteId) {
        List<Integer> emotes = loadEmotes(username);
        emotes.remove(Integer.valueOf(emoteId));
        saveEmotes(username, emotes);
    }

    /**
     * Load all player documents and return them as a map of username -> emote list.
     * Useful for bulk-loading on server startup.
     */
    public java.util.Map<String, List<Integer>> loadAll() {
        java.util.Map<String, List<Integer>> result = new java.util.HashMap<>();
        for (Document doc : playersCollection.find()) {
            String id = doc.getString("_id");
            List<Integer> emotes = new ArrayList<>();
            List<?> raw = doc.getList("emotes", Integer.class);
            if (raw != null) {
                for (Object o : raw) {
                    if (o instanceof Number) {
                        emotes.add(((Number) o).intValue());
                    }
                }
            }
            result.put(id, emotes);
        }
        return result;
    }

    public void close() {
        if (client != null) {
            client.close();
            LOGGER.info("MongoDB connection closed.");
        }
    }
}