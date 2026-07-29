package com.cheatbreaker.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * Reconstructed CheatBreaker WebSocket Server.
 * Handles client connections for cosmetics, friends, emotes, voice chat, and notifications.
 *
 * Protocol: Binary packets over WebSocket (Netty PacketBuffer / ByteBuf).
 * Packet format: [int packetId][payload...]
 */
public class WebSocketServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketServer.class);

    private final int port;
    private final SessionManager sessionManager;
    private final MongoStorage mongoStorage;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Thread commandThread;

    public WebSocketServer(int port, MongoStorage mongoStorage) {
        this.port = port;
        this.mongoStorage = mongoStorage;
        this.sessionManager = new SessionManager(mongoStorage);
    }

    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new WebSocketServerInitializer(sessionManager))
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true);

        Channel channel = bootstrap.bind(port).sync().channel();
        LOGGER.info("CheatBreaker WebSocket Server started on port {}", port);
        LOGGER.info("Session manager ready. Waiting for client connections...");

        // Start console command handler on a daemon thread
        CommandHandler commandHandler = new CommandHandler(sessionManager);
        commandThread = new Thread(commandHandler, "CommandHandler");
        commandThread.setDaemon(true);
        commandThread.start();

        channel.closeFuture().sync();
    }

    public void stop() {
        if (workerGroup != null) workerGroup.shutdownGracefully();
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (mongoStorage != null) mongoStorage.close();
        LOGGER.info("Server stopped.");
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public static void main(String[] args) throws InterruptedException {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port: " + args[0] + ", using default 8080");
            }
        }

        // Load MongoDB config from config.properties (falls back to defaults)
        String mongoUri = "mongodb://localhost:27017";
        String mongoDb = "tellinq";
        try (InputStream in = new FileInputStream("config.properties")) {
            Properties props = new Properties();
            props.load(in);
            mongoUri = props.getProperty("mongo.uri", mongoUri);
            mongoDb = props.getProperty("mongo.db", mongoDb);
            LOGGER.info("Loaded config.properties: mongo.uri={}, mongo.db={}", mongoUri, mongoDb);
        } catch (Exception e) {
            LOGGER.warn("config.properties not found, using defaults: {} / {}", mongoUri, mongoDb);
        }

        MongoStorage mongoStorage = null;
        try {
            mongoStorage = new MongoStorage(mongoUri, mongoDb);
        } catch (Exception e) {
            LOGGER.error("Failed to connect to MongoDB at {}. Emotes will NOT persist across restarts!", mongoUri, e);
            LOGGER.error("Make sure MongoDB is running, or edit config.properties.");
        }

        WebSocketServer server = new WebSocketServer(port, mongoStorage);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();
    }
}