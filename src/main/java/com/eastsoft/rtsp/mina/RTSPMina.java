package com.eastsoft.rtsp.mina;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
/**
 * RTSPMina
 * @author ljt
 * @date 2014-9-10 19:27:01
 *
 */
public class RTSPMina {
	private String ip="218.204.223.237";
	public static void main(String[] args) {
		new RTSPMina().connect();
	}
	
	public void connect() {
		IoConnector connector = new NioSocketConnector();
		connector.getFilterChain().addLast(
				"codec",
				new ProtocolCodecFilter(new MyCodecFactory(new DataEncoder(),
						new DataDecoder())));
		connector.setHandler(new RTSPHandler());
		ConnectFuture future= connector.connect(new InetSocketAddress(ip, 554));
		boolean flag=future.awaitUninterruptibly(10*1000L, TimeUnit.MILLISECONDS);
		System.out.println("connect result:"+flag);

	}
}
