package com.cheatbreaker.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Console command handler for the WebSocket server.
 * Runs on a separate thread, reading commands from stdin.
 *
 * Commands:
 *   emote add <player> <emote>    - Grant an emote to a player (by username)
 *   emote remove <player> <emote> - Remove an emote from a player
 *   emote play <player> <emote>   - Force a player to play an emote (broadcast to friends)
 *   emote list                    - List all available emotes
 *   emote player <player>         - Show a player's owned emotes
 *   players                       - List all connected players
 *   help                          - Show all commands
 */
public class CommandHandler implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandHandler.class);

    private final SessionManager sessionManager;
    private volatile boolean running = true;

    public CommandHandler(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void run() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        LOGGER.info("Command handler started. Type 'help' for available commands.");

        while (running) {
            try {
                System.out.print("> ");
                String line = reader.readLine();
                if (line == null) break;
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] args = line.split("\\s+");
                String command = args[0].toLowerCase();

                switch (command) {
                    case "emote":
                        handleEmoteCommand(args);
                        break;
                    case "players":
                        handlePlayersCommand();
                        break;
                    case "help":
                        printHelp();
                        break;
                    case "stop":
                        LOGGER.info("Shutting down...");
                        running = false;
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Unknown command: " + command + ". Type 'help' for commands.");
                        break;
                }
            } catch (Exception e) {
                LOGGER.error("Error processing command", e);
            }
        }
    }

    private void handleEmoteCommand(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: emote <add|remove|play|list|player> ...");
            return;
        }

        String subCommand = args[1].toLowerCase();

        switch (subCommand) {
            case "add": {
                // emote add <playerName> <emoteName>
                if (args.length < 4) {
                    System.out.println("Usage: emote add <playerName> <emoteName>");
                    System.out.println("Example: emote add Steve dab");
                    return;
                }
                String playerName = args[2];
                String emoteName = args[3];

                Integer emoteId = EmoteRegistry.getId(emoteName);
                if (emoteId == null) {
                    System.out.println("Unknown emote: " + emoteName);
                    System.out.println("Available emotes:");
                    System.out.print(EmoteRegistry.listAll());
                    return;
                }

                Session session = sessionManager.getSessionByUsername(playerName);
                if (session == null) {
                    System.out.println("Player not online: " + playerName);
                    System.out.println("Online players: " + sessionManager.getOnlineUsernames());
                    return;
                }

                sessionManager.addPlayerEmote(session.getPlayerId(), emoteId);
                System.out.println("Granted emote '" + EmoteRegistry.getName(emoteId) + "' (id " + emoteId + ") to " + playerName);

                // Send updated cosmetics/emotes to the player
                sendUpdatedEmotes(session);
                break;
            }

            case "remove": {
                // emote remove <playerName> <emoteName>
                if (args.length < 4) {
                    System.out.println("Usage: emote remove <playerName> <emoteName>");
                    return;
                }
                String playerName = args[2];
                String emoteName = args[3];

                Integer emoteId = EmoteRegistry.getId(emoteName);
                if (emoteId == null) {
                    System.out.println("Unknown emote: " + emoteName);
                    return;
                }

                Session session = sessionManager.getSessionByUsername(playerName);
                if (session == null) {
                    System.out.println("Player not online: " + playerName);
                    return;
                }

                sessionManager.removePlayerEmote(session.getPlayerId(), emoteId);
                System.out.println("Removed emote '" + EmoteRegistry.getName(emoteId) + "' from " + playerName);
                sendUpdatedEmotes(session);
                break;
            }

            case "play": {
                // emote play <playerName> <emoteName>
                if (args.length < 4) {
                    System.out.println("Usage: emote play <playerName> <emoteName>");
                    return;
                }
                String playerName = args[2];
                String emoteName = args[3];

                Integer emoteId = EmoteRegistry.getId(emoteName);
                if (emoteId == null) {
                    System.out.println("Unknown emote: " + emoteName);
                    return;
                }

                Session session = sessionManager.getSessionByUsername(playerName);
                if (session == null) {
                    System.out.println("Player not online: " + playerName);
                    return;
                }

                // Broadcast emote to all friends (and the player themselves)
                WSPacketEmote emotePacket = new WSPacketEmote(session.getPlayerId(), emoteId);
                session.sendPacket(emotePacket);
                sessionManager.broadcastToFriends(session.getPlayerId(), emotePacket);
                System.out.println("Forced " + playerName + " to play emote '" + EmoteRegistry.getName(emoteId) + "'");
                break;
            }

            case "list": {
                System.out.println("Available emotes:");
                System.out.print(EmoteRegistry.listAll());
                break;
            }

            case "player": {
                // emote player <playerName>
                if (args.length < 3) {
                    System.out.println("Usage: emote player <playerName>");
                    return;
                }
                String playerName = args[2];
                Session session = sessionManager.getSessionByUsername(playerName);
                if (session == null) {
                    System.out.println("Player not online: " + playerName);
                    return;
                }

                Set<Integer> emotes = sessionManager.getPlayerEmotes(session.getPlayerId());
                System.out.println("Emotes for " + playerName + " (" + session.getPlayerId() + "):");
                if (emotes.isEmpty()) {
                    System.out.println("  (none)");
                } else {
                    for (int id : emotes) {
                        System.out.println("  " + id + " = " + EmoteRegistry.getName(id));
                    }
                }
                break;
            }

            default:
                System.out.println("Unknown emote subcommand: " + subCommand);
                System.out.println("Usage: emote <add|remove|play|list|player> ...");
                break;
        }
    }

    private void sendUpdatedEmotes(Session session) {
        Set<Integer> emotes = sessionManager.getPlayerEmotes(session.getPlayerId());
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (int emoteId : emotes) {
            if (!first) json.append(",");
            json.append("{\"playerId\":\"").append(session.getPlayerId())
                .append("\",\"emoteId\":").append(emoteId)
                .append(",\"type\":\"emote\"}");
            first = false;
        }
        json.append("]");
        session.sendPacket(new WSPacketCosmetics(json.toString()));
    }

    private void handlePlayersCommand() {
        Collection<Session> sessions = sessionManager.getAllSessions();
        System.out.println("Connected players (" + sessions.size() + "):");
        for (Session s : sessions) {
            System.out.println("  " + (s.getUsername() != null ? s.getUsername() : "?")
                + " | UUID: " + (s.getPlayerId() != null ? s.getPlayerId() : "not registered")
                + " | Auth: " + s.isAuthenticated());
        }
    }

    private void printHelp() {
        System.out.println("=== CheatBreaker WebSocket Server Commands ===");
        System.out.println("  emote add <player> <emote>     - Grant an emote to a player");
        System.out.println("  emote remove <player> <emote>  - Remove an emote from a player");
        System.out.println("  emote play <player> <emote>    - Force play an emote (broadcast)");
        System.out.println("  emote list                     - List all available emotes");
        System.out.println("  emote player <player>          - Show player's owned emotes");
        System.out.println("  players                        - List connected players");
        System.out.println("  stop                           - Stop the server");
        System.out.println("  help                           - Show this help");
        System.out.println();
        System.out.println("Emote names: wave, handsup, floss, dab, tpose, shrug, facepalm, narutorun, superfacepalm");
    }

    public void stop() {
        running = false;
    }
}
