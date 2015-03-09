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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.TreeMap;

import com.kneelawk.stree.packet.listener.ConnectionListener;
import com.kneelawk.stree.packet.listener.DisconnectionListener;
import com.kneelawk.stree.packet.listener.PacketListener;

public class PacketServerConnection {
	protected ServerSocket socket;
	protected TreeMap<SCD, PacketConnection> packetConnections;
	protected ArrayList<ConnectionListener> connectionListeners;
	protected ArrayList<PacketListener> toAddPacketListeners;
	protected ArrayList<NPL> toAddNamedPacketListeners;
	protected TreeMap<SCD, ArrayList<PacketListener>> toAddIPdPacketListeners;
	protected TreeMap<SCD, ArrayList<NPL>> toAddIPdNamedPacketListeners;
	protected ArrayList<DisconnectionListener> toAddDisconnectListeners;
	protected boolean running = false;

	public PacketServerConnection(ServerSocket serverSocket) {
		socket = serverSocket;
		packetConnections = new TreeMap<SCD, PacketConnection>();
		connectionListeners = new ArrayList<ConnectionListener>();
		toAddPacketListeners = new ArrayList<PacketListener>();
		toAddNamedPacketListeners = new ArrayList<NPL>();
		toAddIPdPacketListeners = new TreeMap<SCD, ArrayList<PacketListener>>();
		toAddIPdNamedPacketListeners = new TreeMap<SCD, ArrayList<NPL>>();
		toAddDisconnectListeners = new ArrayList<DisconnectionListener>();
	}

	public void addConnectionListener(ConnectionListener connListener) {
		connectionListeners.add(connListener);
	}

	public void removeConnectionListener(ConnectionListener connListener) {
		connectionListeners.remove(connListener);
	}

	public void addPacketListener(PacketListener listener) {
		Collection<PacketConnection> conns = packetConnections.values();
		for (PacketConnection conn : conns) {
			conn.addPacketListener(listener);
		}
		toAddPacketListeners.add(listener);
	}

	public void addPacketListenerForNames(PacketListener listener,
			String... names) {
		Collection<PacketConnection> conns = packetConnections.values();
		for (PacketConnection conn : conns) {
			conn.addPacketListenerForNames(listener, names);
		}
		toAddNamedPacketListeners.add(new NPL(listener, names));
	}

	public void addPacketListenerForIP(PacketListener listener, InetAddress ip,
			int port) {
		PacketConnection conn = packetConnections.get(new SCD(ip, port));
		conn.addPacketListener(listener);
		ArrayList<PacketListener> listeners = toAddIPdPacketListeners
				.get(new SCD(ip, port));
		boolean createNew = listeners == null;
		if (createNew)
			listeners = new ArrayList<PacketListener>();
		listeners.add(listener);
		if (createNew)
			toAddIPdPacketListeners.put(new SCD(ip, port), listeners);
	}

	public void addPacketListenerForIPAndNames(PacketListener listener,
			InetAddress ip, int port, String... names) {
		PacketConnection conn = packetConnections.get(new SCD(ip, port));
		conn.addPacketListenerForNames(listener, names);
		ArrayList<NPL> listeners = toAddIPdNamedPacketListeners.get(new SCD(ip,
				port));
		boolean createNew = listeners == null;
		if (createNew)
			listeners = new ArrayList<NPL>();
		listeners.add(new NPL(listener, names));
		if (createNew)
			toAddIPdNamedPacketListeners.put(new SCD(ip, port), listeners);
	}

	public void removePacketListener(PacketListener listener) {
		Collection<PacketConnection> conns = packetConnections.values();
		for (PacketConnection conn : conns) {
			conn.removePacketListener(listener);
		}
		toAddPacketListeners.remove(listener);
	}

	public void removePacketListenerForNames(PacketListener listener,
			String... names) {
		Collection<PacketConnection> conns = packetConnections.values();
		for (PacketConnection conn : conns) {
			conn.removePacketListenerForNames(listener, names);
		}
		toAddNamedPacketListeners.remove(new NPL(listener, names));
	}

	public void removePacketListenerForIP(PacketListener listener,
			InetAddress ip, int port) {
		PacketConnection conn = packetConnections.get(new SCD(ip, port));
		conn.removePacketListener(listener);
		ArrayList<PacketListener> listeners = toAddIPdPacketListeners
				.get(new SCD(ip, port));
		if (listeners != null) {
			listeners.remove(listener);
			if (listeners.size() < 1)
				toAddIPdPacketListeners.remove(new SCD(ip, port));
		}
	}

	public void removePacketListenerForIPAndNames(PacketListener listener,
			InetAddress ip, int port, String... names) {
		PacketConnection conn = packetConnections.get(new SCD(ip, port));
		conn.removePacketListenerForNames(listener, names);
		ArrayList<NPL> listeners = toAddIPdNamedPacketListeners.get(new SCD(ip,
				port));
		if (listeners != null) {
			listeners.remove(new NPL(listener, names));
			if (listeners.size() < 1)
				toAddIPdNamedPacketListeners.remove(new SCD(ip, port));
		}
	}

	public void addDisconnectionListener(DisconnectionListener listener) {
		Collection<PacketConnection> coll = packetConnections.values();
		for (PacketConnection conn : coll) {
			conn.addDisconnectionListener(listener);
		}
		toAddDisconnectListeners.add(listener);
	}

	public void removeDisconnectionListener(DisconnectionListener listener) {
		Collection<PacketConnection> coll = packetConnections.values();
		for (PacketConnection conn : coll) {
			conn.removeDisconnectionListener(listener);
		}
		toAddDisconnectListeners.remove(listener);
	}

	public PacketServerConnection start() {
		running = true;
		Thread connListen = new Thread(new Runnable() {
			@Override
			public void run() {
				while (running) {
					Socket clientSocket = null;
					try {
						clientSocket = socket.accept();
						clientSocket.setTcpNoDelay(true);
					} catch (SocketException e) {
					} catch (IOException e) {
						e.printStackTrace();
					}
					if (clientSocket != null) {
						SCD desc = new SCD(clientSocket.getInetAddress(),
								clientSocket.getPort());
						PacketConnection conn = null;
						try {
							conn = new PacketConnection(clientSocket);
						} catch (IOException e) {
							e.printStackTrace();
						}
						if (conn != null) {
							addListeners(conn, desc);
							conn.addDisconnectionListener(new DisconnectionListener() {
								@Override
								public void onSocketDisconnect(Socket socket,
										PacketConnection connection) {
									packetConnections.remove(new SCD(socket
											.getInetAddress(), socket.getPort()));
								}
							});
							packetConnections.put(desc, conn);
							conn.start();
							alertListeners(clientSocket, conn);
						}
					}
				}
			}
		}, "ConnectionListener");
		connListen.start();
		return this;
	}

	protected void addListeners(PacketConnection conn, SCD desc) {
		for (PacketListener listener : toAddPacketListeners) {
			conn.addPacketListener(listener);
		}
		for (NPL pair : toAddNamedPacketListeners) {
			conn.addPacketListenerForNames(pair.listener, pair.names);
		}
		ArrayList<PacketListener> listeners = toAddIPdPacketListeners.get(desc);
		if (listeners != null)
			for (PacketListener listener : listeners)
				conn.addPacketListener(listener);
		ArrayList<NPL> namedListeners = toAddIPdNamedPacketListeners.get(desc);
		if (listeners != null)
			for (NPL pair : namedListeners)
				conn.addPacketListenerForNames(pair.listener, pair.names);
		for (DisconnectionListener listener : toAddDisconnectListeners) {
			conn.addDisconnectionListener(listener);
		}
	}

	protected void alertListeners(Socket socket, PacketConnection conn) {
		for (ConnectionListener listener : connectionListeners) {
			listener.onReceiveConnection(socket, conn);
		}
	}

	public void stop() throws IOException {
		running = false;
		Collection<PacketConnection> conns = packetConnections.values();
		for (PacketConnection conn : conns) {
			conn.stop();
		}
		socket.close();
	}

	public ServerSocket getSocket() {
		return socket;
	}

	public void sendPacket(Packet packet) throws IOException {
		Collection<PacketConnection> conns = packetConnections.values();
		for (PacketConnection conn : conns) {
			conn.sendPacket(packet);
		}
	}

	public void sendPacket(Packet packet, InetAddress ip, int port)
			throws IOException {
		packetConnections.get(new SCD(ip, port)).sendPacket(packet);
	}

	/**
	 * SDC: Socket Connection Description
	 * @author jedidiah
	 *
	 */
	protected static class SCD implements Comparable<SCD> {
		public String address;
		public int port;

		public SCD(String address, int port) {
			this.address = address.substring(address.lastIndexOf('/') + 1,
					address.length());
			this.port = port;
		}

		public SCD(InetAddress address, int port) {
			String addressStr = address.toString();
			this.address = addressStr.substring(
					addressStr.lastIndexOf('/') + 1, addressStr.length());
			this.port = port;
		}

		@Override
		public int compareTo(SCD o) {
			int result = address.compareTo(o.address);
			if (result == 0) {
				if (port > o.port) {
					return 1;
				} else if (port < o.port) {
					return -1;
				} else {
					return 0;
				}
			}
			return result;
		}
	}

	/**
	 * NPL: Named Packet Listener
	 * @author jedidiah
	 *
	 */
	protected static class NPL {
		public PacketListener listener;
		public String[] names;

		public NPL(PacketListener listener, String... names) {
			this.listener = listener;
			this.names = names;
		}

		public boolean equals(Object o) {
			if (o instanceof NPL) {
				return listener.equals(((NPL) o).listener)
						&& Arrays.deepEquals(names, ((NPL) o).names);
			}
			return false;
		}
	}
}
