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


//监听任务状态的websocket接口
//{task_id}:需要监听的任务id
@ServerEndpoint("/tasknotify-websocket/{task_id}")
public class TaskNotifyWebSocket {
	Logger logger = Logger.getInstance();
	
	@OnOpen
    public void onOpen(@PathParam("task_id") String task_id,Session session) 
	{
		if(ServiceManager.setTaskListener(task_id,session) == false) {
			logger.info("TaskNotifyWebSocket opened failedly. task_id="+task_id);
			try {
				session.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			logger.info("TaskNotifyWebSocket opened. task_id="+task_id);
		}
    }
	
	@OnMessage
    public void onMessage(String message,Session session) {
        
    }
	
	@OnClose
    public void onClose(Session session) {
        
    }
	
	@OnError
    public void onError(Session session, Throwable error) {
        
    }
}
