

import java.nio.ByteBuffer;

import org.json.JSONObject;

public class Helper {
	public static ByteBuffer mergeJsonAndData(JSONObject json,byte[] data) {
		byte[] json_bytes = json.toString().getBytes();
	    ByteBuffer buffer = ByteBuffer.allocate(json_bytes.length+data.length+4);
	    buffer.putInt(json_bytes.length);
	    buffer.put(json_bytes);
	    buffer.put(data);
	    buffer.flip();
	    return buffer;
	}
}
