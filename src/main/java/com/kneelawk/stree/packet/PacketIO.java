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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.kneelawk.stree.core.MapSTreeNode;
import com.kneelawk.stree.core.STreeIO;

public class PacketIO {
	public static Packet readPacket(InputStream is) throws IOException {
		return Packet.readPacket((MapSTreeNode) STreeIO
				.readSTreeNodeFromStream(is));
	}

	public static void writePacket(OutputStream os, Packet packet)
			throws IOException {
		STreeIO.writeSTreeNodeToStream(os, Packet.writePacket(packet));
	}

	public static Packet readCompressedPacket(InputStream is)
			throws IOException {
		return readPacket(new GZIPInputStream(is));
	}

	public static void writeCompressedPacket(OutputStream os, Packet packet)
			throws IOException {
		writePacket(new GZIPOutputStream(os), packet);
	}
}
