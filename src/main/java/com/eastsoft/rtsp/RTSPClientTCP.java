package com.eastsoft.rtsp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;

import com.eastsoft.rtsp.RTSPComm.Status;
import com.eastsoft.rtsp.util.Utils;

public class RTSPClientTCP {
	private static final int PORT = 554;
//	private static String IP = "129.1.6.89";
	 private static String IP = "218.204.223.237";
	 private static final String connStr =
	 "rtsp://218.204.223.237:554/live/1/66251FC11353191F/e7ooqwcfbqjoo80j.sdpu";//
//	private static final String connStr = "rtsp://129.1.6.89:554/mpeg4cif";
	private static final String VERSION = " RTSP/1.0";
	private static Socket socket = null;
	private static OutputStream os = null;
	private static InputStream in = null;
	private static int seq = 1;
	private String sessionid="";

	public static void main(String[] args) {
		try {
			socket = new Socket(IP, PORT);
			if (socket != null) {
				os = socket.getOutputStream();
				in = socket.getInputStream();
				doOption();
				readReply();
				doDescribe();
				readReply();
			}
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void doOption() throws UnsupportedEncodingException,
			IOException {

		StringBuilder sb = new StringBuilder();
		// sb.append("OPTIONS ");
		// sb.append(connStr);
		//
		// sb.append(VERSION);
		// sb.append("\r\n");
		//
		// sb.append("Cseq: ");
		// sb.append(2);
		// sb.append("\r\n");
		// sb.append("User-Agent: LibVLC/2.1.5 (LIVE555 Streaming Media v2014.05.27)");
		// sb.append("\r\n");
		// sb.append("\r\n");
		
		sb.append("OPTIONS " + connStr + " RTSP/1.0\r\n");
		sb.append("CSeq: " + (seq++) + "\r\n");
		sb.append("User-Agent: LibVLC/2.1.5 (LIVE555 Streaming Media v2014.05.27)\r\n\r\n");
		System.out.println(sb.toString());
//		sb.append("OPTIONS "+connStr+" RTSP/1.0\r\n");
//		sb.append("CSeq: 2\r\n");
//		sb.append("User-Agent: LibVLC/2.1.5 (LIVE555 Streaming Media v2014.05.27)\r\n\r\n");
//		System.out.println(sb.toString());
		os.write(sb.toString().getBytes("UTF-8"));
	}

	public static void doDescribe() throws UnsupportedEncodingException,
			IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("DESCRIBE "+connStr+" RTSP/1.0\r\n");
		sb.append("CSeq: 3\r\n");
		sb.append("User-Agent: LibVLC/2.1.5 (LIVE555 Streaming Media v2014.05.27)\r\n\r\n");
		System.out.println(sb.toString());
		os.write(sb.toString().getBytes("UTF-8"));
	}
	
	public void doSetup() throws Exception, IOException {
		
		StringBuilder sb = new StringBuilder();
		sb.append("SETUP " + connStr + "/track1 RTSP/1.0\r\n");
		sb.append("CSeq: 4\r\n");		
		sb.append("User-Agent: LibVLC/2.1.5 (LIVE555 Streaming Media v2014.05.27)\r\n");
		sb.append("Transport: RTP/AVP/TCP;unicast;client_port=54684-54685\r\n\r\n");
		System.out.println(sb.toString());
		os.write(sb.toString().getBytes("UTF-8"));

	}
	
	public void doPlay() throws Exception, IOException {
		
		StringBuilder sb = new StringBuilder();
		sb.append("PLAY " + connStr + " RTSP/1.0\r\n");
		sb.append("CSeq: 5\r\n");		
		sb.append("User-Agent: LibVLC/2.1.5 (LIVE555 Streaming Media v2014.05.27)\r\n");
		sb.append("Session: " + sessionid + "\r\n");
		sb.append("Range: npt=0.000-\r\n\r\n");
		System.out.println(sb.toString());
		os.write(sb.toString().getBytes("UTF-8"));


	}

	public static void readReply() throws IOException {
		int len = 0;
		byte[] b = new byte[1024];
		StringBuffer sb = new StringBuffer();
//		in.available()
		while ((len = in.read(b))!= -1) {
			sb.append(new String(b, 0, len, "UTF-8"));
		}
		System.out.println("reply=" + sb.toString());
	}

}
