package jfilebeat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.dtstack.jfilebeat.common.utils.TimeUtils;

public class TestFd {

	public static void createFile(String filepath) {
		File f = new File(filepath);
		try {
			f.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException {
		addlog();
	}

	public static void addlog() throws IOException {
		Map<String, RandomAccessFile> m = new HashMap<String, RandomAccessFile>();

		File dir = new File("/tmp/log/");
		for (File f : dir.listFiles()) {

			RandomAccessFile rf = new RandomAccessFile(f.getAbsolutePath(), "rw");
			
			for(int i = 0; i < 100; i++) {
				String line = TimeUtils.utc(new Date()) + "\n";
				FileChannel fc = rf.getChannel();
				ByteBuffer byteBuf = ByteBuffer.wrap(line.getBytes());

				fc.position(fc.size());
				int c = fc.write(byteBuf);
				byteBuf.clear();
				fc.force(false);
			}
			
			rf.close();
			
		}

	}

	public static void test() throws IOException {
		RandomAccessFile rf = new RandomAccessFile("/tmp/log/test/0.log", "rw");
		String line = "yyyyyyyyy\n";

		FileChannel fc = rf.getChannel();
		ByteBuffer byteBuf = ByteBuffer.wrap(line.getBytes());

		fc.write(byteBuf);
		byteBuf.clear();
		System.out.println(line);

	}
}
