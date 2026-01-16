import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;


@ServerEndpoint("/service-websocket/{service_name}/{password}")
public class ServiceWebSocket {
	
	Logger logger = Logger.getInstance();
	
	@OnOpen
    public void onOpen(@PathParam("service_name") String service_name,
    					@PathParam("password") String password,
    				    Session session) 
	{
		logger.info("WebSocket onOpen("+service_name);
		// 验证一级密码
		if(! password.equals("1d2f3ghduwaijiajdiJAU82131fsdfjuji")) {
			try {
				logger.info("Password Error."+password);
				session.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		session.setMaxBinaryMessageBufferSize(50 * 1024 * 1024);  // 50MB
	    session.setMaxTextMessageBufferSize(64 * 1024);      // 64KB
		ServiceManager.addService(service_name,session);
		try {
			//发送初始消息
			session.getBasicRemote().sendText("{\"service_id\":\""+session.getId()+"\"}");
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
	
//	@OnMessage
//    public void onMessage(Session session, ByteBuffer byteBuffer) {
//        byte[] data = new byte[byteBuffer.remaining()];
//        byteBuffer.get(data);
//        ServiceManager.dispatchMessage(data);
//    }
	
	@OnMessage
    public void onBinaryMessage(byte[] data, Session session) {
		ServiceManager.dispatchMessage(data);
	}
	
	@OnMessage
	public void onTextMessage(String message, Session session) {
	    System.out.println("Text message: " + message);
	}
	
	@OnClose
    public void onClose(Session session) {
        ServiceManager.dropService(session);
    }
	
	@OnError
    public void onError(Session session, Throwable error) {
        logger.error(session.getId()+".onError("+error.toString());
        if(session.isOpen() == false) {
	        ServiceManager.dropService(session);
        }
    }
	
    public static void sendFile(Session session, String filepath) throws IOException {
        // 1. 读取文件
        byte[] fileData = Files.readAllBytes(Paths.get(filepath));
        
        // 2. 作为二进制消息发送                      
        ByteBuffer buffer = ByteBuffer.wrap(fileData);
        session.getBasicRemote().sendBinary(buffer);
    }
}
