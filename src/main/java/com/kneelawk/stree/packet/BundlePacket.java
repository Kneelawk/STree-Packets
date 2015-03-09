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

import java.util.ArrayList;

import com.kneelawk.stree.packet.infoProviders.BundlePacketInfoProvider;
import com.kneelawk.stree.packet.infoProviders.PacketInfoProvider;

public class BundlePacket extends Packet {

	public ArrayList<Packet> packets;

	public BundlePacket() {
		packets = new ArrayList<Packet>();
	}

	public BundlePacket(ArrayList<Packet> packets) {
		this.packets = packets;
	}

	public BundlePacket(String name) {
		super(name);
		packets = new ArrayList<Packet>();
	}

	public BundlePacket(ArrayList<Packet> packets, String name) {
		super(name);
		this.packets = packets;
	}

	public ArrayList<Packet> getPackets() {
		return packets;
	}

	public void setPackets(ArrayList<Packet> packets) {
		this.packets = packets;
	}

	@Override
	public PacketInfoProvider getInfoProvider() {
		return new BundlePacketInfoProvider();
	}

	public String toString() {
		String str = "BundlePacket " + name + ":{\n";
		for (Packet pack : packets) {
			str += pack.toString() + "\n";
		}
		str += "}";
		return str;
	}

}
