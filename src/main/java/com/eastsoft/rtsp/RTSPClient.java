package com.eastsoft.rtsp;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringBufferInputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.eastsoft.rtsp.util.Utils;

public class RTSPClient extends Thread implements IEvent {

	private static final String VERSION = " RTSP/1.0\r\n";
	private static final String RTSP_OK = "RTSP/1.0 200 OK";

	/** */
	/** 远程地址 */
	private final InetSocketAddress remoteAddress;

	/** */
	/** * 本地地址 */
	private final InetSocketAddress localAddress;

	/** */
	/** * 连接通道 */
	private SocketChannel socketChannel;

	/** */
	/** 发送缓冲区 */
	private final ByteBuffer sendBuf;

	/** */
	/** 接收缓冲区 */
	private final ByteBuffer receiveBuf;

	private static final int BUFFER_SIZE = 8192;

	/** */
	/** 端口选择器 */
	private Selector selector;

	private String address;

	private Status sysStatus;

	private String sessionid;

	/** */
	/** 线程是否结束的标志 */
	private AtomicBoolean shutdown;

	private int seq = 1;

	private boolean isSended;

	private String trackInfo;
	
	private String response;
	private Map<String, String> map;
	private String username = "admin";
	private String passwd = "123456";
	private String uri = "rtsp://129.1.6.89/mpeg4cif";

	private enum Status {
		init, options, describe, auth, setup, play,playing, pause, teardown
	}

	public RTSPClient(InetSocketAddress remoteAddress,
			InetSocketAddress localAddress, String address) {
		this.remoteAddress = remoteAddress;
		this.localAddress = localAddress;
		this.address = address;

		// 初始化缓冲区
		sendBuf = ByteBuffer.allocateDirect(BUFFER_SIZE);
		receiveBuf = ByteBuffer.allocateDirect(BUFFER_SIZE);
		if (selector == null) {
			// 创建新的Selector
			try {
				selector = Selector.open();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}

		startup();
		sysStatus = Status.init;
		shutdown = new AtomicBoolean(false);
		isSended = false;
	}

	public void startup() {
		try {
			// 打开通道
			socketChannel = SocketChannel.open();
			// 绑定到本地端口
			socketChannel.socket().setSoTimeout(30000);
			socketChannel.configureBlocking(false);
			socketChannel.socket().bind(localAddress);
			if (socketChannel.connect(remoteAddress)) {
				System.out.println("开始建立连接:" + remoteAddress);
			}
			socketChannel.register(selector, SelectionKey.OP_CONNECT
					| SelectionKey.OP_READ | SelectionKey.OP_WRITE, this);
			System.out.println("端口打开成功");

		} catch (final IOException e1) {
			e1.printStackTrace();
		}
	}

	public static void main(String[] args) {
		try {
			// RTSPClient(InetSocketAddress remoteAddress,
			// InetSocketAddress localAddress, String address)
//			RTSPClient client = new RTSPClient(new InetSocketAddress(
//					"129.1.6.89", 554),
//					new InetSocketAddress("129.1.77.14", 0),
//					"rtsp://129.1.6.89:554/mpeg4cif");

			 RTSPClient client = new RTSPClient(new InetSocketAddress(
			 "218.204.223.237", 554),
			 new InetSocketAddress("129.1.77.16", 0),
			 "rtsp://218.204.223.237:554/live/1/66251FC11353191F/e7ooqwcfbqjoo80j.sdpu");
			client.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void send(byte[] out) {
		if (out == null || out.length < 1) {
			return;
		}
		synchronized (sendBuf) {
			sendBuf.clear();
			sendBuf.put(out);
			sendBuf.flip();
		}

		// 发送出去
		try {
			write();
			isSended = true;
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public void write() throws IOException {
		if (isConnected()) {
			try {
				socketChannel.write(sendBuf);
			} catch (final IOException e) {
			}
		} else {
			System.out.println("通道为空或者没有连接上");
		}
	}

	public byte[] recieve() {
		if (isConnected()) {
			try {
				int len = 0;
				int readBytes = 0;

				synchronized (receiveBuf) {
					receiveBuf.clear();
					try {
						while ((len = socketChannel.read(receiveBuf)) > 0) {
							readBytes += len;
						}
					} finally {
						receiveBuf.flip();
					}
					if (readBytes > 0) {
						final byte[] tmp = new byte[readBytes];
						receiveBuf.get(tmp);
						return tmp;
					} else {
						System.out.println("接收到数据为空,重新启动连接");
						return null;
					}
				}
			} catch (final IOException e) {
				System.out.println("接收消息错误:");
			}
		} else {
			System.out.println("端口没有连接");
		}
		return null;
	}

	public boolean isConnected() {
		return socketChannel != null && socketChannel.isConnected();
	}

	private void select() {
		int n = 0;
		try {
			if (selector == null) {
				return;
			}
			n = selector.select(1000);

		} catch (final Exception e) {
			e.printStackTrace();
		}

		// 如果select返回大于0，处理事件
		if (n > 0) {
			for (final Iterator<SelectionKey> i = selector.selectedKeys()
					.iterator(); i.hasNext();) {
				// 得到下一个Key
				final SelectionKey sk = i.next();
				i.remove();
				// 检查其是否还有效
				if (!sk.isValid()) {
					continue;
				}

				// 处理事件
				final IEvent handler = (IEvent) sk.attachment();
				try {
					if (sk.isConnectable()) {
						handler.connect(sk);
					} else if (sk.isReadable()) {
						handler.read(sk);
					} else {
						// System.err.println("Ooops");
					}
				} catch (final Exception e) {
					handler.error(e);
					sk.cancel();
				}
			}
		}
	}

	public void shutdown() {
		if (isConnected()) {
			try {
				socketChannel.close();
				System.out.println("端口关闭成功");
			} catch (final IOException e) {
				System.out.println("端口关闭错误:");
			} finally {
				socketChannel = null;
			}
		} else {
			System.out.println("通道为空或者没有连接");
		}
	}

	@Override
	public void run() {
		// 启动主循环流程
		while (!shutdown.get()) {
			try {
				if (isConnected() && (!isSended)) {
					switch (sysStatus) {
					case init:
						doOption();
						break;
					case options:
						doDescribe();
						break;
					case auth:
						break;
					case describe:												
						doSetup();
						break;
					case setup:
						if (sessionid == null && sessionid.length() > 0) {
							System.out.println("setup还没有正常返回");
						} else {
							doPlay();
						}
						break;
					case play:
						doPause();
						break;
					case playing:
						
						break;
					case pause:
						doTeardown();
						break;
					default:
						break;
					}
				}
				// do select
				select();
				try {
					Thread.sleep(1000);
				} catch (final Exception e) {
					e.printStackTrace();
				}
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}

		shutdown();
	}

	public void connect(SelectionKey key) throws IOException {
		if (isConnected()) {
			return;
		}
		// 完成SocketChannel的连接
		socketChannel.finishConnect();
		while (!socketChannel.isConnected()) {
			try {
				Thread.sleep(300);
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
			socketChannel.finishConnect();
		}

	}

	public void error(Exception e) {
		e.printStackTrace();
	}

	public void read(SelectionKey key) throws IOException {
		// 接收消息
		final byte[] msg = recieve();
		if (msg != null) {
			handle(msg);
		} else {
			key.cancel();
		}
	}

	private void handle(byte[] msg) {
		String tmp = new String(msg);
		System.out.println("返回内容：");
		System.out.println(tmp);
		// if (tmp.startsWith(RTSP_OK)) {
		switch (sysStatus) {
		case init:
			sysStatus = Status.options;
			break;
		case options:
			if (tmp.contains("Unauthorized")) {
				map = Utils.parseUnauth(tmp);
				String realm = map.get("realm");
				String nonce = map.get("nonce");
				String md1 = Utils.makeMD5(username + ":" + realm + ":"
						+ passwd);
				response = Utils.makeMD5(md1 + ":" + nonce + ":"
						+ Utils.makeMD5("DESCRIBE" + ":" + uri));
				doAuth(username, nonce, realm, response);
				sysStatus = Status.auth;
			}else{
				sysStatus=Status.describe;
			}
			break;
		case auth:
			sysStatus = Status.describe;
			break;
		case describe:
			sessionid = Utils.parseSession(tmp);
			if (sessionid != null && sessionid.length() > 0) {
				sysStatus = Status.setup;
			}
			break;
		case setup:
			sysStatus = Status.play;
			break;
		case play:
			sysStatus = Status.playing;
			break;
		case pause:
			sysStatus = Status.teardown;
			shutdown.set(true);
			break;
		case teardown:
			sysStatus = Status.init;
			break;
		default:
			break;
		}
		isSended = false;
		// } else {
		// System.out.println("返回错误：" + tmp);
		// }

	}

	private void doAuth(String username, String nonce, String realm,
			String respone) {
		StringBuilder sb = new StringBuilder();
		sb.append("DESCRIBE " + this.address + " RTSP/1.0\r\n");
		sb.append("CSeq: " + (seq++) + "\r\n");
		sb.append("Authorization: Digest username=\""+username+"\", realm=\""+realm+"\", nonce=\""+nonce+"\", uri=\"rtsp://129.1.6.89/mpeg4cif\", response=\""+respone+"\"\r\n");
		sb.append("User-Agent: LibVLC/2.1.5 (LIVE555 Streaming Media v2014.05.27)\r\n");
		sb.append("Accept: application/sdp\r\n\r\n");
		System.out.println("doAuth:" + sb.toString());
		try {
			send(sb.toString().getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void doTeardown() {
		StringBuilder sb = new StringBuilder();
		sb.append("TEARDOWN ");
		sb.append(this.address);
		sb.append("/");
		sb.append(VERSION);
		sb.append("Cseq: ");
		sb.append(seq++);
		sb.append("\r\n");
		sb.append("User-Agent: RealMedia Player HelixDNAClient/10.0.0.11279 (win32)\r\n");
		sb.append("Session: ");
		sb.append(sessionid);
		sb.append("\r\n");
		send(sb.toString().getBytes());
		System.out.println(sb.toString());
	}

	private void doPlay() {
//		String realm = map.get("realm");
//		String nonce = map.get("nonce");
//		String md1 = Utils.makeMD5(username + ":" + realm + ":"
//				+ passwd);
//		response = Utils.makeMD5(md1 + ":" + nonce + ":"
//				+ Utils.makeMD5("PLAY" + ":" + uri));
		StringBuilder sb = new StringBuilder();	
		sb.append("PLAY " + this.address + " RTSP/1.0\r\n");
		sb.append("CSeq: " + (seq++) + "\r\n");
//		sb.append("Authorization: Digest username=\""+username+"\", realm=\""+realm+"\", nonce=\""+nonce+"\", uri=\"rtsp://129.1.6.89/mpeg4cif\", response=\""+response+"\"\r\n");
		sb.append("User-Agent: LibVLC/2.1.5 (LIVE555 Streaming Media v2014.05.27)\r\n");
		sb.append("Session: "+sessionid+"\r\n");
		sb.append("Range: npt=0.000-\r\n\r\n");
		System.out.println(sb.toString());
		try {
			send(sb.toString().getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

	}

	private void doSetup() {
		// String response = "8d53a436f6bb678ba6f7020f515ec28e";
//		String realm = map.get("realm");
//		String nonce = map.get("nonce");
//		String md1 = Utils.makeMD5(username + ":" + realm + ":"
//				+ passwd);
//		response = Utils.makeMD5(md1 + ":" + nonce + ":"
//				+ Utils.makeMD5("SETUP" + ":" + uri));
		StringBuilder sb = new StringBuilder();
		sb.append("SETUP " + this.address + "/track1 RTSP/1.0\r\n");
		sb.append("CSeq: " + (seq++) + "\r\n");
//		sb.append("Authorization: Digest username=\""+username+"\", realm=\""+realm+"\", nonce=\""+nonce+"\", uri=\"rtsp://129.1.6.89/mpeg4cif\", response=\""+response+"\"\r\n");
		sb.append("User-Agent: LibVLC/2.1.5 (LIVE555 Streaming Media v2014.05.27)\r\n");
		sb.append("Transport: RTP/AVP;unicast;client_port=54684-54685\r\n\r\n");
		System.out.println(sb.toString());
		try {
			send(sb.toString().getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void doOption() {
		StringBuilder sb = new StringBuilder();
		sb.append("OPTIONS " + this.address + " RTSP/1.0\r\n");
		sb.append("CSeq: " + (seq++) + "\r\n");
		sb.append("User-Agent: LibVLC/2.1.5 (LIVE555 Streaming Media v2014.05.27)\r\n\r\n");
		System.out.println(sb.toString());
		
		try {
			send(sb.toString().getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public byte[] toByteArray(String hexStr) {
		String[] s = hexStr.split(" ");
		byte[] bb = new byte[s.length];
		for (int i = 0; i < s.length; i++) {
			bb[i] = Byte.parseByte(s[i], 16);
		}
		return bb;
	}

	private void doDescribe() {
		StringBuilder sb = new StringBuilder();
		sb.append("DESCRIBE " + this.address + " RTSP/1.0\r\n");
		sb.append("CSeq: " + (seq++) + "\r\n");
		sb.append("Accept: application/sdp\r\n");
		sb.append("User-Agent: LibVLC/2.1.5 (LIVE555 Streaming Media v2014.05.27)\r\n\r\n");
		System.out.println(sb.toString());
		try {
			send(sb.toString().getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void doPause() {
		StringBuilder sb = new StringBuilder();

		// sb.append("PAUSE " + this.address + " RTSP/1.0\r\n");
		// sb.append("CSeq: "+(seq++)+"\r\n");
		// sb.append("Accept: application/sdp\r\n");
		// sb.append("User-Agent: LibVLC/2.1.5 (LIVE555 Streaming Media v2014.05.27)\r\n\r\n");

		sb.append("PAUSE ");
		sb.append(this.address);
		sb.append("/");
		sb.append(VERSION);
		sb.append("Cseq: ");
		sb.append(seq++);
		sb.append("\r\n");
		sb.append("Session: ");
		sb.append(sessionid);
		sb.append("\r\n");
		send(sb.toString().getBytes());
		System.out.println(sb.toString());
	}

}
