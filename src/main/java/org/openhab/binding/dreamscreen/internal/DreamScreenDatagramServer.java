/**
 * Copyright (c) 2018-2019 by the respective copyright holders.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.dreamscreen.internal;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.net.CidrAddress;
import org.eclipse.smarthome.core.net.NetworkAddressChangeListener;
import org.eclipse.smarthome.core.net.NetworkAddressService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link DreamScreenDatagramServer} class handles all communications with the DreamScreen devices.
 *
 * @author Bruce Brouwer
 */
class DreamScreenDatagramServer implements NetworkAddressChangeListener {
    private final static int DREAMSCREEN_PORT = 8888;
    private final static byte[] CRC_TABLE = new byte[] { 0x00, 0x07, 0x0E, 0x09, 0x1C, 0x1B, 0x12, 0x15, 0x38, 0x3F,
            0x36, 0x31, 0x24, 0x23, 0x2A, 0x2D, 0x70, 0x77, 0x7E, 0x79, 0x6C, 0x6B, 0x62, 0x65, 0x48, 0x4F, 0x46, 0x41,
            0x54, 0x53, 0x5A, 0x5D, (byte) 0xE0, (byte) 0xE7, (byte) 0xEE, (byte) 0xE9, (byte) 0xFC, (byte) 0xFB,
            (byte) 0xF2, (byte) 0xF5, (byte) 0xD8, (byte) 0xDF, (byte) 0xD6, (byte) 0xD1, (byte) 0xC4, (byte) 0xC3,
            (byte) 0xCA, (byte) 0xCD, (byte) 0x90, (byte) 0x97, (byte) 0x9E, (byte) 0x99, (byte) 0x8C, (byte) 0x8B,
            (byte) 0x82, (byte) 0x85, (byte) 0xA8, (byte) 0xAF, (byte) 0xA6, (byte) 0xA1, (byte) 0xB4, (byte) 0xB3,
            (byte) 0xBA, (byte) 0xBD, (byte) 0xC7, (byte) 0xC0, (byte) 0xC9, (byte) 0xCE, (byte) 0xDB, (byte) 0xDC,
            (byte) 0xD5, (byte) 0xD2, (byte) 0xFF, (byte) 0xF8, (byte) 0xF1, (byte) 0xF6, (byte) 0xE3, (byte) 0xE4,
            (byte) 0xED, (byte) 0xEA, (byte) 0xB7, (byte) 0xB0, (byte) 0xB9, (byte) 0xBE, (byte) 0xAB, (byte) 0xAC,
            (byte) 0xA5, (byte) 0xA2, (byte) 0x8F, (byte) 0x88, (byte) 0x81, (byte) 0x86, (byte) 0x93, (byte) 0x94,
            (byte) 0x9D, (byte) 0x9A, 0x27, 0x20, 0x29, 0x2E, 0x3B, 0x3C, 0x35, 0x32, 0x1F, 0x18, 0x11, 0x16, 0x03,
            0x04, 0x0D, 0x0A, 0x57, 0x50, 0x59, 0x5E, 0x4B, 0x4C, 0x45, 0x42, 0x6F, 0x68, 0x61, 0x66, 0x73, 0x74, 0x7D,
            0x7A, (byte) 0x89, (byte) 0x8E, (byte) 0x87, (byte) 0x80, (byte) 0x95, (byte) 0x92, (byte) 0x9B,
            (byte) 0x9C, (byte) 0xB1, (byte) 0xB6, (byte) 0xBF, (byte) 0xB8, (byte) 0xAD, (byte) 0xAA, (byte) 0xA3,
            (byte) 0xA4, (byte) 0xF9, (byte) 0xFE, (byte) 0xF7, (byte) 0xF0, (byte) 0xE5, (byte) 0xE2, (byte) 0xEB,
            (byte) 0xEC, (byte) 0xC1, (byte) 0xC6, (byte) 0xCF, (byte) 0xC8, (byte) 0xDD, (byte) 0xDA, (byte) 0xD3,
            (byte) 0xD4, 0x69, 0x6E, 0x67, 0x60, 0x75, 0x72, 0x7B, 0x7C, 0x51, 0x56, 0x5F, 0x58, 0x4D, 0x4A, 0x43, 0x44,
            0x19, 0x1E, 0x17, 0x10, 0x05, 0x02, 0x0B, 0x0C, 0x21, 0x26, 0x2F, 0x28, 0x3D, 0x3A, 0x33, 0x34, 0x4E, 0x49,
            0x40, 0x47, 0x52, 0x55, 0x5C, 0x5B, 0x76, 0x71, 0x78, 0x7F, 0x6A, 0x6D, 0x64, 0x63, 0x3E, 0x39, 0x30, 0x37,
            0x22, 0x25, 0x2C, 0x2B, 0x06, 0x01, 0x08, 0x0F, 0x1A, 0x1D, 0x14, 0x13, (byte) 0xAE, (byte) 0xA9,
            (byte) 0xA0, (byte) 0xA7, (byte) 0xB2, (byte) 0xB5, (byte) 0xBC, (byte) 0xBB, (byte) 0x96, (byte) 0x91,
            (byte) 0x98, (byte) 0x9F, (byte) 0x8A, (byte) 0x8D, (byte) 0x84, (byte) 0x83, (byte) 0xDE, (byte) 0xD9,
            (byte) 0xD0, (byte) 0xD7, (byte) 0xC2, (byte) 0xC5, (byte) 0xCC, (byte) 0xCB, (byte) 0xE6, (byte) 0xE1,
            (byte) 0xE8, (byte) 0xEF, (byte) 0xFA, (byte) 0xFD, (byte) 0xF4, (byte) 0xF3 };

    private final Logger logger = LoggerFactory.getLogger(DreamScreenHandler.class);
    private final Map<String, DreamScreenHandler> dreamScreens = new ConcurrentHashMap<>();

    private @Nullable NetworkAddressService networkAddressService;
    private @Nullable Thread server;
    private @Nullable DatagramSocket socket;
    private @Nullable InetAddress hostAddress;
    private @Nullable InetAddress broadcastAddress;

    void initialize(@NonNull DreamScreenHandler handler) throws IOException {
        dreamScreens.put(handler.name, handler);
        if (!isServerRunning()) {
            startServer();
        }
    }

    void dispose(@NonNull DreamScreenHandler handler) {
        dreamScreens.remove(handler.name);
        if (dreamScreens.isEmpty()) {
            stopServer();
        }
    }

    void deactivate() {
        dreamScreens.clear();
        stopServer();
    }

    private void runServer() {
        final byte[] data = new byte[256];
        DatagramSocket socket = this.socket;

        while (socket != null && !socket.isClosed()) {
            try {
                final DatagramPacket packet = new DatagramPacket(data, data.length);
                socket.receive(packet);

                final int off = packet.getOffset();
                final int len = packet.getLength();

                logger.trace("DreamScreen message: {} {}-{}:{}", packet.getAddress(), off, len, data);

                if (!packet.getAddress().equals(this.hostAddress) && isValidMsg(data, off, len)) {
                    logger.debug("Received DreamScreen message from {}: {}, {}", packet.getAddress(), data[off + 4],
                            data[off + 5]);
                    if (isRefreshMsg(data, off, len)) {
                        processRefreshMsg(data, off, len, packet.getAddress());
                    } else {
                        processMessage(data, off, len);
                    }
                }
            } catch (IOException ioe) {
                logger.error("Error receiving DreamScreen data", ioe);
            }
        }
    }

    private boolean isValidMsg(final byte[] data, int off, int len) {
        if (len > 6 && data[off] == (byte) 0xFC) {
            final int msgLen = data[off + 1] & 0xFF;
            if (msgLen + 2 > len) {
                return false; // invalid length
            } else if (data[off + msgLen + 1] != calcCRC8(data, off)) {
                return false; // invalid crc
            }
            return true;
        }
        return false; // message not long enough
    }

    private boolean isRefreshMsg(final byte[] data, int off, int len) {
        if (len > 77) {
            final int msgLen = data[off + 1] & 0xFF;
            final int upperCommand = data[off + 4];
            final int lowerCommand = data[off + 5];
            final int productId = data[off + msgLen];

            return msgLen > 77 && upperCommand == 0x01 && lowerCommand == 0x0A && productId > 0 && productId <= 2;
        }
        return false;
    }

    private void processRefreshMsg(final byte[] data, int off, int len, InetAddress address) {
        final String name = new String(data, off + 6, 16, StandardCharsets.UTF_8).trim();
        final DreamScreenHandler dreamScreen = this.dreamScreens.get(name);

        if (dreamScreen != null) {
            dreamScreen.address = address;
            dreamScreen.group = data[off + 38];
            dreamScreen.processMessage(data, off, len);
        }
    }

    private void processMessage(final byte[] data, int off, int len) {
        final byte group = data[2];
        for (DreamScreenHandler dreamscreen : this.dreamScreens.values()) {
            if (group == 0 || dreamscreen.group == group) {
                dreamscreen.processMessage(data, off, len);
            }
        }
    }

    void broadcast(int group, int commandUpper, int commandLower, byte[] payload) throws IOException {
        broadcast(group, 0b00100001, commandUpper, commandLower, payload);
    }

    void broadcast(int group, int flags, int commandUpper, int commandLower, byte[] payload) throws IOException {
        send(group, flags, commandUpper, commandLower, payload, this.broadcastAddress);
    }

    void send(int group, int commandUpper, int commandLower, byte[] payload, InetAddress address) throws IOException {
        send(group, 0b00010001, commandUpper, commandLower, payload, this.broadcastAddress);
    }

    void send(int group, int flags, int commandUpper, int commandLower, byte[] payload, InetAddress address)
            throws IOException {
        DatagramSocket socket = this.socket;
        if (socket != null) {
            byte[] msg = buildMsg(group, flags, commandUpper, commandLower, payload);
            socket.send(new DatagramPacket(msg, msg.length, address, DREAMSCREEN_PORT));
            // processMessage(msg, 0, msg.length);
        } else {
            logger.warn("Message not sent because the server is not running");
        }
    }

    private byte[] buildMsg(int group, int flags, int commandUpper, int commandLower, byte[] payload) {
        final byte[] msg = new byte[payload.length + 7];
        msg[0] = (byte) 0xFC;
        msg[1] = (byte) (0x05 + payload.length);
        msg[2] = (byte) group;
        msg[3] = (byte) flags;
        msg[4] = (byte) commandUpper;
        msg[5] = (byte) commandLower;
        System.arraycopy(payload, 0, msg, 6, payload.length);
        msg[payload.length + 6] = calcCRC8(msg, 0);
        return msg;
    }

    private static final byte calcCRC8(byte[] data, int off) {
        int size = (data[off + 1] & 0xFF) + 1;
        int cntr = 0;
        byte crc = 0x00;
        while (cntr < size) {
            crc = CRC_TABLE[(byte) (crc ^ (data[off + cntr])) & 0xFF];
            cntr++;
        }
        return crc;
    }

    private boolean isServerRunning() {
        final DatagramSocket socket = this.socket;
        final Thread server = this.server;
        return socket != null && !socket.isClosed() && server != null;
    }

    private void startServer() throws IOException {
        final DatagramSocket socket = new DatagramSocket(DREAMSCREEN_PORT, hostAddress);
        socket.setBroadcast(true);
        socket.setReuseAddress(true);
        this.socket = socket;

        final Thread server = new Thread(this::runServer, "dreamscreen-tv");
        server.setDaemon(true);
        server.start();
        this.server = server;
    }

    private void stopServer() {
        final DatagramSocket socket = this.socket;
        final Thread server = this.server;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        if (server != null) {
            try {
                server.join(5000);
            } catch (InterruptedException e) {
                logger.error("Failed to wait for server to stop", e);
            }
        }
    }

    void setNetworkAddressService(@NonNull NetworkAddressService networkAddressService) {
        this.networkAddressService = networkAddressService;
        networkAddressService.addNetworkAddressChangeListener(this);
        try {
            configureNetwork();
        } catch (IOException e) {
            logger.error("Unable to configure the network", e);
        }
    }

    void unsetNetworkAddressService(@NonNull NetworkAddressService networkAddressService) {
        networkAddressService.removeNetworkAddressChangeListener(this);
        this.networkAddressService = null;
    }

    @Override
    public void onChanged(List<@NonNull CidrAddress> added, List<@NonNull CidrAddress> removed) {
        try {
            configureNetwork();
            if (!this.dreamScreens.isEmpty()) {
                stopServer();
                startServer();
            }
        } catch (IOException ioe) {
            logger.error("Cannot configure new network settings", ioe);
        }
    }

    private void configureNetwork() throws IOException {
        final NetworkAddressService networkAddressService = this.networkAddressService;
        if (networkAddressService == null) {
            throw new IOException("No network address service found");
        }
        final String host = networkAddressService.getPrimaryIpv4HostAddress();
        if (host == null) {
            throw new IOException("No primary IPv4 host address could be found");
        }
        final InetAddress hostAddress = InetAddress.getByName(host);
        this.hostAddress = hostAddress;

        final String broadcast = networkAddressService.getConfiguredBroadcastAddress();
        if (broadcast != null) {
            this.broadcastAddress = InetAddress.getByName(broadcast);
            return;
        }

        // no valid broadcast address configured. Try to determine a valid one to use
        final byte[] address = hostAddress.getAddress();
        final NetworkInterface net = NetworkInterface.getByInetAddress(hostAddress);
        final int prefixLen = net.getInterfaceAddresses().get(0).getNetworkPrefixLength();
        final int prefixIndex = prefixLen / 8;
        address[prefixIndex] |= 0xFF >> ((prefixLen - prefixIndex * 8) % 8);
        for (int i = prefixIndex + 1; i < address.length; i++) {
            address[i] = (byte) 0xFF;
        }
        this.broadcastAddress = InetAddress.getByAddress(address);
    }
}
