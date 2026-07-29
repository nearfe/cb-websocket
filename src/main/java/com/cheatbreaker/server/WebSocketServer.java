package com.cheatbreaker.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Thread commandThread;

    public WebSocketServer(int port) {
        this.port = port;
        this.sessionManager = new SessionManager();
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

        WebSocketServer server = new WebSocketServer(port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();
    }
}
