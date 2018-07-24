package com.dtstack.jfilebeat.net;

import com.dtstack.jfilebeat.common.Event;
import com.dtstack.jfilebeat.common.logger.Log;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;

/**
 * 协议格式：PROTOCOL_VERSION (1 bytes) | FRAME_WINDOW_SIZE (1 bytes) | window_size
 * (4 bytes) | PROTOCOL_VERSION (1 bytes) | FRAME_COMPRESSED (1 bytes) |
 * compressedData.size (4 bytes) | compressedData (PROTOCOL_VERSION | FRAME_DATA
 * | sequence | keyValues.size | keyValues )
 * 
 * @author daguan
 *
 */
public class NetClient {

	private Log logger = Log.getLogger(getClass());

	private Socket socket;
	private String host;
	private int port;
	private DataOutputStream output;
	private DataInputStream input;
	private int sequence = 1;
	private int timeout;

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public void reconnect() throws IOException {

		close();
		connect();
	}

	public NetClient(String host, int port, int timeout) {
		this.host = host;
		this.port = port;
		this.timeout = timeout;
	}

	public void connect() throws IOException {
		logger.info("connect start, host={}, port={}, timeout={}", host, port, timeout);

		if (host == null) {
			throw new IOException("Server address not configured");
		}
		socket = new Socket();
		socket.connect(new InetSocketAddress(InetAddress.getByName(host), port), timeout);
		socket.setSoTimeout(timeout);

		output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		input = new DataInputStream(socket.getInputStream());
	}

	public int sendWindowSizeFrame(int size) throws IOException {
		output.writeByte(Protocol.VERSION_1);
		output.writeByte(Protocol.CODE_WINDOW_SIZE);
		output.writeInt(size);
		output.flush();

		return 6;
	}

	public int sendCompressedFrame(List<Map<String, byte[]>> keyValuesList) throws IOException {
		output.writeByte(Protocol.VERSION_1);
		output.writeByte(Protocol.CODE_COMPRESSED_FRAME);

		ByteArrayOutputStream uncompressedBytes = new ByteArrayOutputStream();
		DataOutputStream uncompressedOutput = new DataOutputStream(uncompressedBytes);
		for (Map<String, byte[]> keyValues : keyValuesList) {
			sendDataFrame(uncompressedOutput, keyValues);
		}
		uncompressedOutput.close();
		Deflater compressor = new Deflater();
		byte[] uncompressedData = uncompressedBytes.toByteArray();
		compressor.setInput(uncompressedData);
		compressor.finish();

		ByteArrayOutputStream compressedBytes = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		while (!compressor.finished()) {
			int count = compressor.deflate(buffer);
			compressedBytes.write(buffer, 0, count);
		}
		compressedBytes.close();
		byte[] compressedData = compressedBytes.toByteArray();

		output.writeInt(compressor.getTotalOut());
		output.write(compressedData);
		output.flush();

		return 6 + compressor.getTotalOut();
	}

	private int sendDataFrame(DataOutputStream output, Map<String, byte[]> keyValues) throws IOException {
		output.writeByte(Protocol.VERSION_1);
		output.writeByte(Protocol.CODE_FRAME_DATA);
		output.writeInt(sequence++);
		output.writeInt(keyValues.size());
		int bytesSent = 10;
		for (String key : keyValues.keySet()) {
			int keyLength = key.length();
			output.writeInt(keyLength);
			bytesSent += 4;
			output.write(key.getBytes());
			bytesSent += keyLength;
			byte[] value = keyValues.get(key);
			output.writeInt(value.length);
			bytesSent += 4;
			output.write(value);
			bytesSent += value.length;
		}
		output.flush();
		return bytesSent;
	}

	public int send(List<Event> eventList) throws IOException {
		sequence = 1;
		int numberOfEvents = eventList.size();
		sendWindowSizeFrame(numberOfEvents);
		List<Map<String, byte[]>> keyValuesList = new ArrayList<Map<String, byte[]>>(numberOfEvents);
		for (Event event : eventList) {
			keyValuesList.add(event.getKeyValues());
		}
		int byteCount = sendCompressedFrame(keyValuesList);
		while (readAckFrame() < (sequence - 1)) {
		}
		return byteCount;
	}

	public int readAckFrame() throws ProtocolException, IOException {
		byte protocolVersion = input.readByte();
		
		if(protocolVersion == Protocol.VERSION_2) {
			input.readByte();
			input.readInt();
			return readAckFrame();
		}
		
		if (protocolVersion != Protocol.VERSION_1) {
			throw new ProtocolException("Protocol version should be 1, received " + protocolVersion);
		}
		
		byte frameType = input.readByte();
		if (frameType != Protocol.CODE_FRAME_ACK) {
			throw new ProtocolException("Frame type should be Ack, received " + frameType);
		}
		int sequenceNumber = input.readInt();
		logger.debug("readAckFrame, sequenceNumber={}, seq={}", sequenceNumber, sequence);
		return sequenceNumber;
	}

	public void close() {

		try {
			if (socket != null) {
				socket.close();
				socket = null;
			}

		} catch (Exception e) {
			logger.error("close error", e);
		}
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

}
