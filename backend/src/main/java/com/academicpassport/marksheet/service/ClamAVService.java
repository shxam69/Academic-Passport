package com.academicpassport.marksheet.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Service
public class ClamAVService {

    private static final Logger log = LoggerFactory.getLogger(ClamAVService.class);

    private final String host;
    private final int port;
    private final int timeoutMs;
    private final int bufferSize = 8192; // 8KB chunks

    public ClamAVService(
            @Value("${clamav.host:clamav}") String host,
            @Value("${clamav.port:3310}") int port,
            @Value("${clamav.timeout:10000}") int timeoutMs) {
        this.host = host;
        this.port = port;
        this.timeoutMs = timeoutMs;
    }

    /**
     * Scans the input stream using ClamAV's INSTREAM protocol.
     * Throws an exception if the stream is infected or ClamAV is unavailable.
     */
    public void scan(InputStream inputStream) {
        try (Socket socket = new Socket()) {
            socket.setSoTimeout(timeoutMs);
            socket.connect(new InetSocketAddress(host, port), timeoutMs);

            try (BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
                 InputStream in = socket.getInputStream()) {

                // Send nINSTREAM command
                out.write("nINSTREAM\n".getBytes(StandardCharsets.US_ASCII));
                out.flush();

                byte[] buffer = new byte[bufferSize];
                int read;
                while ((read = inputStream.read(buffer)) >= 0) {
                    if (read > 0) {
                        // Write chunk length in network byte order (Big Endian)
                        byte[] lengthHeader = ByteBuffer.allocate(4).putInt(read).array();
                        out.write(lengthHeader);
                        // Write chunk data
                        out.write(buffer, 0, read);
                    }
                }
                
                // Write zero-length chunk to signal EOF
                out.write(ByteBuffer.allocate(4).putInt(0).array());
                out.flush();

                // Read response
                byte[] responseBuffer = new byte[256];
                int responseRead = in.read(responseBuffer);
                if (responseRead <= 0) {
                    throw new com.academicpassport.common.ServiceUnavailableException("ClamAV closed connection without response");
                }
                
                String response = new String(responseBuffer, 0, responseRead, StandardCharsets.US_ASCII).trim();
                log.info("ClamAV Response: {}", response);

                if (response.endsWith("OK") && !response.contains("FOUND")) {
                    // Clean
                    return;
                } else if (response.contains("FOUND")) {
                    throw new SecurityException("Malware detected: " + response);
                } else {
                    throw new com.academicpassport.common.ServiceUnavailableException("Unexpected ClamAV response: " + response);
                }

            }
        } catch (SecurityException e) {
            throw e; // Rethrow business-logic rejection
        } catch (IOException e) {
            log.error("ClamAV scan failed (timeout/unavailable)", e);
            throw new com.academicpassport.common.ServiceUnavailableException("Virus scanning service unavailable", e);
        }
    }
}
