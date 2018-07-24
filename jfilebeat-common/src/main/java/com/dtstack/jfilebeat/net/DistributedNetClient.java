package com.dtstack.jfilebeat.net;

import com.dtstack.jfilebeat.common.Event;
import com.dtstack.jfilebeat.common.logger.Log;
import com.dtstack.jfilebeat.common.thread.NamedThreadFactory;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DistributedNetClient {
	
	private List<String> servers;
	
	private int timeoutMs;
	
	private Log logger = Log.getLogger(getClass());
	
	private List<NetClient> clients = new CopyOnWriteArrayList<NetClient>();
	
	private List<NetClient> healthlessServers = new CopyOnWriteArrayList<NetClient>();
	
	private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1,
			new NamedThreadFactory("jfilebeat-netclient"));
	
	private int defaultTimeoutMs = 60000;
	
	public DistributedNetClient(List<String> servers, int timeoutMs) {
		
		logger.info("init,servers={}, timeoutMs={}", servers, timeoutMs);
		this.servers = servers;
		this.timeoutMs = timeoutMs;
	}
	
	public void init() {
		for(String s : servers) {
			String[] ss = s.split(":");
			NetClient client = new NetClient(ss[0], Integer.valueOf(ss[1]), timeoutMs > 0? timeoutMs : defaultTimeoutMs);
			try {
				client.connect();
				clients.add(client);
			} catch (IOException e) {
				logger.warn("connect error, host={}", client.getHost());
				logger.error("connect error", e);
				healthlessServers.add(client);
			}
		}
		
		scheduler.scheduleWithFixedDelay(new Runnable() {

			public void run() {
				healthCheck();
			}
		}, 10, 10, TimeUnit.SECONDS);
	}
	
	public void healthCheck() {
		
		logger.debug("healthCheck start, healthlessServers={}", healthlessServers);
		
		for (NetClient c : healthlessServers) {
			try {
				c.reconnect();
				healthlessServers.remove(c);
				clients.add(c);
				
			} catch (Exception e) {
				logger.warn("healthCheck error, host={}", c.getHost());
				logger.error("healthCheck error", e);
			}
		}

	}


	public int hash() {
		return (int) (Math.random() * 10000) % clients.size();
	}

	private NetClient getOneClient() {
		if(clients.size() == 0) {
			return null;
		}
		return clients.get(hash());
	}
	
	public int send(List<Event> eventList) {
		
		
		if(eventList.size() == 0) {
			return 0;
		}
		
		while(true) {
			
			NetClient c = getOneClient();
			
			if(c == null) {
				logger.warn("client is empty");
				try {
					Thread.sleep(4000l);
				} catch (InterruptedException e) {
				}
				continue;
			}
			
			try {
				return c.send(eventList);
			} catch (Exception e) {
				logger.warn("send error, host={}", c.getHost());
				logger.error("send error", e);
				
				healthlessServers.add(c);
				clients.remove(c);
			}
		}
	}
	
	public void close() {
		
		scheduler.shutdown();
		
		for(NetClient c : clients) {
			c.close();
		}
	}

}
