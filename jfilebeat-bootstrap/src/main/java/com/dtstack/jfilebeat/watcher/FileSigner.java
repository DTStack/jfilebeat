package com.dtstack.jfilebeat.watcher;

import java.io.File;
import java.io.IOException;
import java.util.zip.Adler32;
import com.dtstack.jfilebeat.common.file.RandomAccessFile;

public class FileSigner {
	
	private static final Adler32 adler32 = new Adler32();
	
	private static final long defaultSignatureLength = 4096;
	
	public static long computeSignature(RandomAccessFile file, int signatureLength) throws IOException {
		adler32.reset();
		byte[] input = new byte[signatureLength];
		file.seek(0);
		file.read(input);
		adler32.update(input);
		return adler32.getValue();
	}
	
	public static long computeSignature(File file) throws IOException {
		long len = file.length() > defaultSignatureLength? defaultSignatureLength : file.length();
		RandomAccessFile rfile = new RandomAccessFile(file.getAbsolutePath(), "r");
		long signer = computeSignature(rfile, (int)len);
		rfile.close();
		return signer;
	}
}
