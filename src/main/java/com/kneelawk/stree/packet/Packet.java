/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Kneelawk
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.kneelawk.stree.packet;

import java.io.IOException;
import java.util.TreeMap;

import com.kneelawk.stree.core.MapSTreeNode;
import com.kneelawk.stree.core.StringSTreeNode;
import com.kneelawk.stree.packet.infoProviders.AdvancedPacketInfoProvider;
import com.kneelawk.stree.packet.infoProviders.BundlePacketInfoProvider;
import com.kneelawk.stree.packet.infoProviders.PacketInfoProvider;
import com.kneelawk.stree.packet.infoProviders.SimplePacketInfoProvider;

public abstract class Packet {
	public static TreeMap<String, PacketInfoProvider> infoProviders = createProviderList();

	public String name = "packet";

	public Packet() {
	}

	public Packet(String name) {
		this.name = name;
	}

	private static TreeMap<String, PacketInfoProvider> createProviderList() {
		TreeMap<String, PacketInfoProvider> infos = new TreeMap<String, PacketInfoProvider>();
		addPacketInfoProvider(infos, new AdvancedPacketInfoProvider());
		addPacketInfoProvider(infos, new BundlePacketInfoProvider());
		addPacketInfoProvider(infos, new SimplePacketInfoProvider());
		return infos;
	}

	private static void addPacketInfoProvider(
			TreeMap<String, PacketInfoProvider> infos,
			PacketInfoProvider provider) {
		infos.put(provider.getPacketID(), provider);
	}

	public static void registerInfoProvider(PacketInfoProvider provider) {
		if (infoProviders.containsKey(provider.getPacketID()))
			throw new RuntimeException(
					"There is already a packet registered as: "
							+ provider.getPacketID());
		infoProviders.put(provider.getPacketID(), provider);
	}

	/**
	 * Writes packet to a MapSTreeNode and returns it. The keys: "packetId" and
	 * "packetName" are already used.
	 * 
	 * @param packet
	 *            the Packet to write.
	 * @return a MapSTreeNode of the contents of packet.
	 * @throws IOException
	 */
	public static MapSTreeNode writePacket(Packet packet) throws IOException {
		if (packet == null)
			throw new NullPointerException("Packet cannot be null!");
		MapSTreeNode root = new MapSTreeNode();
		PacketInfoProvider info = packet.getInfoProvider();
		info.write(packet, root);
		root.put("packetId", new StringSTreeNode(info.getPacketID()));
		root.put("packetName", new StringSTreeNode(packet.name));
		return root;
	}

	/**
	 * Reads a Packet from root and returns it. The key: "packetId" is used for
	 * determining which PacketInfoProvider to use to construct the packet; and
	 * the key "packetName" is used for the packet's name.
	 * 
	 * @param root
	 *            the MapSTreeNode to read.
	 * @return a packet described by root.
	 * @throws IOException
	 */
	public static Packet readPacket(MapSTreeNode root) throws IOException {
		if (root == null)
			throw new NullPointerException("Root cannot be null!");
		String id = root.getString("packetId").getValue();
		PacketInfoProvider info = infoProviders.get(id);
		if (info == null)
			throw new IOException("Unknown packet id: " + id);
		Packet packet = info.read(root);
		packet.name = root.getString("packetName").getValue();
		return packet;
	}

	public abstract PacketInfoProvider getInfoProvider();
}
