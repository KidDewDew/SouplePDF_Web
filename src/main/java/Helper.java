

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

import org.json.JSONObject;

public class Helper {

	public static ByteBuffer mergeJsonAndData(JSONObject json,byte[] data) {
		byte[] json_bytes = json.toString().getBytes();
	    ByteBuffer buffer = ByteBuffer.allocate(json_bytes.length+data.length+4);
	    //注：ByteBuffer使用大端序(高字节在低内存)
	    buffer.putInt(json_bytes.length);
	    buffer.put(json_bytes);
	    buffer.put(data);
	    buffer.flip();
	    return buffer;
	}
	public static String generateDateTimeRandom() {
        // 日期
        LocalDateTime now = LocalDateTime.now();
        String dateTimePart = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        
        // 6位随机数
        int randomNum = ThreadLocalRandom.current().nextInt(100000, 1000000);
        String randomPart = String.format("%06d", randomNum);
        
        return dateTimePart + randomPart;
    }
}
