package com.dtstack.jfilebeat.common.bean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {

	public enum TimeConvert {
		MILLISECOND(1, "ms"), SECOND(1000, "s"), MIN(1000 * 60, "m"), HOUR(1000 * 60 * 60, "h");

		private int factor;
		private String keyword;

		public int getFactor() {
			return factor;
		}

		public void setFactor(int factor) {
			this.factor = factor;
		}

		public String getKeyword() {
			return keyword;
		}

		public void setKeyword(String keyword) {
			this.keyword = keyword;
		}

		TimeConvert(int factor, String keyword) {
			this.factor = factor;
			this.keyword = keyword;
		}

		public static int value(String time) {
			for (TimeConvert t : TimeConvert.values()) {
				if (time.contains(t.getKeyword())) {
					return t.getFactor() * Integer.valueOf(time.replace(t.getKeyword(), ""));
				}
			}
			return Integer.valueOf(time);
		}
	}

	public Logging getLogging() {
		return logging;
	}

	public void setLogging(Logging logging) {
		this.logging = logging;
	}

	public static Config config;

	public static Config ins() {
		return config;
	}

	public static void set(Config config) {
		Config.config = config;
	}

	public static class Output {
		private Logstash logstash = new Logstash();
		private Console console = new Console();
		private Map<String, Object> ssl;

		public Logstash getLogstash() {
			return logstash;
		}

		public void setLogstash(Logstash logstash) {
			this.logstash = logstash;
		}

		public Console getConsole() {
			return console;
		}

		public void setConsole(Console console) {
			this.console = console;
		}

		public Map<String, Object> getSsl() {
			return ssl;
		}

		public void setSsl(Map<String, Object> ssl) {
			this.ssl = ssl;
		}
	}

	public static class Console {
		private boolean pretty = false;
		private boolean enabled = false;

		public boolean isPretty() {
			return pretty;
		}

		public void setPretty(boolean pretty) {
			this.pretty = pretty;
		}

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}
	}

	public static class Logstash {

		private List<String> hosts = new ArrayList<String>();
		private int compression_level = 3;
		private long net_max = 0; // bps
		private int cpu_max = 0; // 整型，0到100
		private Boolean enabled;
		private Integer worker;
		private Boolean loadbalance;
		private String index;

		public List<String> getHosts() {
			return hosts;
		}

		public void setHosts(List<String> hosts) {
			this.hosts = hosts;
		}

		public int getCompression_level() {
			return compression_level;
		}

		public void setCompression_level(int compression_level) {
			this.compression_level = compression_level;
		}

		public long getNet_max() {
			return net_max;
		}

		public void setNet_max(long net_max) {
			this.net_max = net_max;
		}

		public int getCpu_max() {
			return cpu_max;
		}

		public void setCpu_max(int cpu_max) {
			this.cpu_max = cpu_max;
		}

		public Boolean getEnabled() {
			return enabled;
		}

		public void setEnabled(Boolean enabled) {
			this.enabled = enabled;
		}

		public Integer getWorker() {
			return worker;
		}

		public void setWorker(Integer worker) {
			this.worker = worker;
		}

		public Boolean getLoadbalance() {
			return loadbalance;
		}

		public void setLoadbalance(Boolean loadbalance) {
			this.loadbalance = loadbalance;
		}

		public String getIndex() {
			return index;
		}

		public void setIndex(String index) {
			this.index = index;
		}
	}

	public static class Logging {
		private boolean to_files = true;
		private String level = "info";
		private Files files = new Files();
		private boolean to_syslog = false;
		private List<String> selectors;

		public boolean isTo_files() {
			return to_files;
		}

		public void setTo_files(boolean to_files) {
			this.to_files = to_files;
		}

		public String getLevel() {
			return level;
		}

		public void setLevel(String level) {
			this.level = level;
		}

		public Files getFiles() {
			return files;
		}

		public void setFiles(Files files) {
			this.files = files;
		}

		public boolean isTo_syslog() {
			return to_syslog;
		}

		public void setTo_syslog(boolean to_syslog) {
			this.to_syslog = to_syslog;
		}

		public List<String> getSelectors() {
			return selectors;
		}

		public void setSelectors(List<String> selectors) {
			this.selectors = selectors;
		}
	}

	public static class Files {
		private String path = "";
		private String max_size = "10MB";
		private int keepfiles = 7;
		private String name = "mybeat.log";
		private Long rotateeverybytes;

		public String getMax_size() {
			return max_size;
		}

		public void setMax_size(String max_size) {
			this.max_size = max_size;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public int getKeepfiles() {
			return keepfiles;
		}

		public void setKeepfiles(int keepfiles) {
			this.keepfiles = keepfiles;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Long getRotateeverybytes() {
			return rotateeverybytes;
		}

		public void setRotateeverybytes(Long rotateeverybytes) {
			this.rotateeverybytes = rotateeverybytes;
		}
	}

	public static class Filebeat {

		private List<Prospector> prospectors = new ArrayList<Prospector>();
		private String registry_file;
		private Integer spool_size = 2048;
		private Boolean publish_async;
		private String idle_timeout = "1s";
		private Integer buffer_size = 1024 * 1024;
		private int idleTimeoutInt = -1;
		private int networkTimeoutInt = -1;
		private String network_timeout = "60s";
		private String config_dir;
		
		public Integer getBuffer_size() {
			return buffer_size;
		}

		public void setBuffer_size(Integer buffer_size) {
			this.buffer_size = buffer_size;
		}

		public int getNetWorkTimeoutInt() {
			if (networkTimeoutInt > 0) {
				return networkTimeoutInt;
			}
			networkTimeoutInt = TimeConvert.value(network_timeout);
			return networkTimeoutInt;
		}

		public int getIdleTimeoutInt() {
			if (idleTimeoutInt > 0) {
				return idleTimeoutInt;
			}
			idleTimeoutInt = TimeConvert.value(idle_timeout);
			return idleTimeoutInt;
		}

		public List<Prospector> getProspectors() {
			return prospectors;
		}

		public void setProspectors(List<Prospector> prospectors) {
			this.prospectors = prospectors;
		}

		public String getRegistry_file() {
			return registry_file;
		}

		public void setRegistry_file(String registry_file) {
			this.registry_file = registry_file;
		}

		public Integer getSpool_size() {
			return spool_size;
		}

		public void setSpool_size(Integer spool_size) {
			this.spool_size = spool_size;
		}

		public Boolean getPublish_async() {
			return publish_async;
		}

		public void setPublish_async(Boolean publish_async) {
			this.publish_async = publish_async;
		}

		public String getIdle_timeout() {
			return idle_timeout;
		}

		public void setIdle_timeout(String idle_timeout) {
			this.idle_timeout = idle_timeout;
		}

		public void setIdleTimeoutInt(int idleTimeoutInt) {
			this.idleTimeoutInt = idleTimeoutInt;
		}

		public int getNetworkTimeoutInt() {
			return networkTimeoutInt;
		}

		public void setNetworkTimeoutInt(int networkTimeoutInt) {
			this.networkTimeoutInt = networkTimeoutInt;
		}

		public String getNetwork_timeout() {
			return network_timeout;
		}

		public void setNetwork_timeout(String network_timeout) {
			this.network_timeout = network_timeout;
		}

		public String getConfig_dir() {
			return config_dir;
		}

		public void setConfig_dir(String config_dir) {
			this.config_dir = config_dir;
		}
	}

	public static class Prospector {
		private Boolean tail_files = true;
		private Boolean fields_under_root = true;
		private List<String> paths = new ArrayList<String>();
		private String input_type = "log";
		private String encoding = "utf-8";
		private List<String> exclude_files;
		private Map<String, Object> fields = new HashMap<String,  Object>();
		private MultiLine multiline;
		private TransactionLine transactionline;
		private List<String> exclude_lines;
		private List<String> include_lines;
		private Long max_bytes;

		public Boolean getTail_files() {
			return tail_files;
		}

		public void setTail_files(Boolean tail_files) {
			this.tail_files = tail_files;
		}

		public Boolean getFields_under_root() {
			return fields_under_root;
		}

		public void setFields_under_root(Boolean fields_under_root) {
			this.fields_under_root = fields_under_root;
		}

		public List<String> getPaths() {
			return paths;
		}

		public void setPaths(List<String> paths) {
			this.paths = paths;
		}

		public String getInput_type() {
			return input_type;
		}

		public void setInput_type(String input_type) {
			this.input_type = input_type;
		}

		public String getEncoding() {
			return encoding;
		}

		public void setEncoding(String encoding) {
			this.encoding = encoding;
		}

		public List<String> getExclude_files() {
			return exclude_files;
		}

		public void setExclude_files(List<String> exclude_files) {
			this.exclude_files = exclude_files;
		}

		public Map<String, Object> getFields() {
			return fields;
		}

		public void setFields(Map<String, Object> fields) {
			this.fields = fields;
		}

		public MultiLine getMultiline() {
			return multiline;
		}

		public void setMultiline(MultiLine multiline) {
			this.multiline = multiline;
		}

		public TransactionLine getTransactionline() {
			return transactionline;
		}

		public void setTransactionline(TransactionLine transactionline) {
			this.transactionline = transactionline;
		}

		public List<String> getExclude_lines() {
			return exclude_lines;
		}

		public void setExclude_lines(List<String> exclude_lines) {
			this.exclude_lines = exclude_lines;
		}

		public List<String> getInclude_lines() {
			return include_lines;
		}

		public void setInclude_lines(List<String> include_lines) {
			this.include_lines = include_lines;
		}

		public Long getMax_bytes() {
			return max_bytes;
		}

		public void setMax_bytes(Long max_bytes) {
			this.max_bytes = max_bytes;
		}
	}

	public static class MultiLine {
		private boolean negate = true;
		private String pattern = "";
		private String match = "after";
		private String timeout = "10s";
		private int max_lines = 500;
		private int timeoutInt = -1;

		public boolean isNegate() {
			return negate;
		}

		public void setNegate(boolean negate) {
			this.negate = negate;
		}

		public String getPattern() {
			return pattern;
		}

		public void setPattern(String pattern) {
			this.pattern = pattern;
		}

		public String getMatch() {
			return match;
		}

		public void setMatch(String match) {
			this.match = match;
		}

		public String getTimeout() {
			return timeout;
		}

		public void setTimeout(String timeout) {
			this.timeout = timeout;
		}

		public int getMax_lines() {
			return max_lines;
		}

		public void setMax_lines(int max_lines) {
			this.max_lines = max_lines;
		}

		public void setTimeoutInt(int timeoutInt) {
			this.timeoutInt = timeoutInt;
		}

		public int getTimeoutInt() {
			if (timeoutInt > 0) {
				return timeoutInt;
			}
			timeoutInt = TimeConvert.value(timeout);
			return timeoutInt;
		}
	}

	public static class TransactionLine {
		private List<String> patterns;
		private String timeout = "20s";
		private int max_lines = 500;
		private String end_flag = "include";
		private int timeoutInt = -1;

		public String getEnd_flag() {
			return end_flag;
		}

		public void setEnd_flag(String end_flag) {
			this.end_flag = end_flag;
		}

		public List<String> getPatterns() {
			return patterns;
		}

		public void setPatterns(List<String> patterns) {
			this.patterns = patterns;
		}

		public String getTimeout() {
			return timeout;
		}

		public void setTimeout(String timeout) {
			this.timeout = timeout;
		}

		public int getMax_lines() {
			return max_lines;
		}

		public void setMax_lines(int max_lines) {
			this.max_lines = max_lines;
		}

		public void setTimeoutInt(int timeoutInt) {
			this.timeoutInt = timeoutInt;
		}

		public int getTimeoutInt() {
			if (timeoutInt > 0) {
				return timeoutInt;
			}
			timeoutInt = TimeConvert.value(timeout);
			return timeoutInt;
		}
	}

	public static class Heartbeat {
		private String url = "-1";
		private int interval = -1;

		public int getInterval() {
			return interval;
		}

		public void setInterval(int interval) {
			this.interval = interval;
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}
	}

	private Output output = new Output();
	private Heartbeat heartbeat = new Heartbeat();
	private Logging logging = new Logging();
	private Filebeat filebeat = new Filebeat();
	private Metric metric = new Metric();
	private String name;
	private List<String> tags;
	private Map<String, String> fields;
	private Boolean fields_under_root;
	private Integer queue_size;
	private Integer bulk_queue_size;
	private Integer max_procs;
	
	public Heartbeat getHeartbeat() {
		return heartbeat;
	}

	public void setHeartbeat(Heartbeat heartbeat) {
		this.heartbeat = heartbeat;
	}

	public static Config getConfig() {
		return config;
	}

	public static void setConfig(Config config) {
		Config.config = config;
	}

	public Output getOutput() {
		return output;
	}

	public void setOutput(Output output) {
		this.output = output;
	}

	public Filebeat getFilebeat() {
		return filebeat;
	}

	public void setFilebeat(Filebeat filebeat) {
		this.filebeat = filebeat;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public Map<String, String> getFields() {
		return fields;
	}

	public void setFields(Map<String, String> fields) {
		this.fields = fields;
	}

	public Boolean getFields_under_root() {
		return fields_under_root;
	}

	public void setFields_under_root(Boolean fields_under_root) {
		this.fields_under_root = fields_under_root;
	}

	public Integer getQueue_size() {
		return queue_size;
	}

	public void setQueue_size(Integer queue_size) {
		this.queue_size = queue_size;
	}

	public Integer getBulk_queue_size() {
		return bulk_queue_size;
	}

	public void setBulk_queue_size(Integer bulk_queue_size) {
		this.bulk_queue_size = bulk_queue_size;
	}

	public Integer getMax_procs() {
		return max_procs;
	}

	public void setMax_procs(Integer max_procs) {
		this.max_procs = max_procs;
	}

	/**
	 * <p>
	 * 性能数据采集
	 * </p>
	 * 
	 * @author 青涯
	 */
	public static class Metric {

		private String encoding = "utf-8";

		private Map<String, String> fields = new HashMap<String, String>();

		public String getEncoding() {
			return encoding;
		}

		public void setEncoding(String encoding) {
			this.encoding = encoding;
		}

		public Map<String, String> getFields() {
			return fields;
		}

		public void setFields(Map<String, String> fields) {
			this.fields = fields;
		}
	}

	public Metric getMetric() {
		return metric;
	}

	public void setMetric(Metric metric) {
		this.metric = metric;
	}
}
