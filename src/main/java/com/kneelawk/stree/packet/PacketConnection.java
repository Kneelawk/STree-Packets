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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.TreeMap;

import com.kneelawk.stree.packet.listener.DisconnectionListener;
import com.kneelawk.stree.packet.listener.PacketListener;
import com.kneelawk.stree.packet.streamProviders.InputStreamProvider;
import com.kneelawk.stree.packet.streamProviders.OutputStreamProvider;
import com.kneelawk.stree.packet.streamProviders.ThroughInputStreamProvider;
import com.kneelawk.stree.packet.streamProviders.ThroughOutputStreamProvider;

public class PacketConnection {

	protected Socket socket;
	protected InputStream is;
	protected OutputStream os;
	protected InputStreamProvider isProvider;
	protected OutputStreamProvider osProvider;
	protected ArrayList<Packet> packetQueue;
	protected ArrayList<PacketListener> listeners;
	protected ArrayList<DisconnectionListener> disconnectListeners;
	protected TreeMap<String, ArrayList<PacketListener>> namedListeners;
	protected boolean disconnect = false;
	protected boolean running = false;

	public PacketConnection(Socket socket) throws IOException {
		this(socket, new ThroughInputStreamProvider(),
				new ThroughOutputStreamProvider());
	}

	public PacketConnection(Socket socket, InputStreamProvider inProv,
			OutputStreamProvider outProv) throws IOException {
		this.socket = socket;
		is = socket.getInputStream();
		os = socket.getOutputStream();
		isProvider = inProv;
		osProvider = outProv;
		listeners = new ArrayList<PacketListener>();
		namedListeners = new TreeMap<String, ArrayList<PacketListener>>();
		packetQueue = new ArrayList<Packet>();
		disconnectListeners = new ArrayList<DisconnectionListener>();
	}

	public void addPacketListener(PacketListener listener) {
		listeners.add(listener);
	}

	public void addPacketListenerForNames(PacketListener listener,
			String... names) {
		for (String name : names) {
			ArrayList<PacketListener> listeners = namedListeners.get(name);
			boolean createNew = listeners == null;
			if (createNew)
				listeners = new ArrayList<PacketListener>();
			listeners.add(listener);
			if (createNew)
				namedListeners.put(name, listeners);
		}
	}

	public boolean removePacketListener(PacketListener listener) {
		return listeners.remove(listener);
	}

	public void removePacketListenerForNames(PacketListener listener,
			String... names) {
		for (String name : names) {
			ArrayList<PacketListener> listeners = namedListeners.get(name);
			if (listeners != null) {
				listeners.remove(listener);
				if (listeners.size() < 1)
					namedListeners.remove(name);
			}
		}
	}

	public void addDisconnectionListener(DisconnectionListener listener) {
		disconnectListeners.add(listener);
	}

	public void removeDisconnectionListener(DisconnectionListener listener) {
		disconnectListeners.remove(listener);
	}

	public PacketConnection start() {
		running = true;
		Thread reader = new Thread(new Runnable() {
			@Override
			public void run() {
				while (running) {
					Packet packet = null;
					try {
						packet = PacketIO.readPacket(isProvider
								.getInputStream(is));
					} catch (SocketException e) {
					} catch (EOFException e) {
						disconnect = true;
						break;
					} catch (IOException e) {
					}

					if (packet != null) {
						packetQueue.add(packet);
					}
				}
			}
		}, "PacketReader");
		Thread queueListener = new Thread(new Runnable() {
			@Override
			public void run() {
				while (running) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (packetQueue.size() > 0) {
						Packet packet = packetQueue.get(0);
						packetQueue.remove(0);
						if (packet != null)
							alertListeners(packet);
					} else {
						if (disconnect) {
							running = false;
							alertDisconnect();
							try {
								is.close();
								os.close();
								socket.close();
							} catch (IOException e2) {
							}
							break;
						}
					}
				}
			}
		}, "EventListener");
		reader.start();
		queueListener.start();
		return this;
	}

	protected void alertDisconnect() {
		for (DisconnectionListener listener : disconnectListeners) {
			listener.onSocketDisconnect(socket, this);
		}
	}

	protected void alertListeners(Packet packet) {
		for (PacketListener listener : listeners) {
			listener.onReceivePacket(packet.name, packet);
		}
		ArrayList<PacketListener> listeners = namedListeners.get(packet.name);
		if (listeners != null)
			for (PacketListener listener : listeners) {
				listener.onReceivePacket(packet.name, packet);
			}
	}

	public void stop() {
		disconnect = true;
	}

	public Socket getSocket() {
		return socket;
	}

	public void sendPacket(Packet packet) throws IOException {
		PacketIO.writePacket(osProvider.getOutputStream(os), packet);
		os.flush();
	}
}
