

//服务管理器

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.websocket.Session;

import org.json.JSONObject;

public class ServiceManager {
	
	//记录每个服务的可用Session列表
	private static final ConcurrentHashMap<String, List<Session>> 
		services = new ConcurrentHashMap<>();
	
	//记录每个session对应的服务名称
	private static final ConcurrentHashMap<String,String>
		session_to_servicename = new ConcurrentHashMap<>();
	
	//记录每个task的Callback
	private static final ConcurrentHashMap<String,Callback>
		tasks = new ConcurrentHashMap<>();
	
	//记录task_id 到 Session的映射
	private static final ConcurrentHashMap<String,Session>
		task_to_session = new ConcurrentHashMap<>();
	
	//记录对任务的listener websocket，好通知前端。
	private static final ConcurrentHashMap<String,Session>
		task_notify_session = new ConcurrentHashMap<>();
	
	private static Logger logger = Logger.getInstance();
	
	static {
		// pdf表格提取服务
		services.put("PDFTableExtract",Collections.synchronizedList(new ArrayList<Session>()));
	}
	
	public static interface Callback {
		// recv接收消息函数
		// session: 服务WebSocket Session
		// json: 消息json
		// raw_data: 原始消息，包含json(前64字节)
		void recv(String task_id,Session session,JSONObject json,byte[] raw_data,int data_pos);
	}
	
	// 获取指定服务的服务列表
	public static List<Session> getServiceList(String service_name) {
		return services.get(service_name);
	}
	
	// 添加服务实例
	public static void addService(String service_name,Session session) {
		List<Session> list = getServiceList(service_name);
		if(! list.contains(session)) {
			list.add(session);
			session_to_servicename.put(session.getId(),service_name);
			logger.info("addService("+service_name+",session:"+session.getId());
		}
	}
	
	
	// 移除服务实例
	public static void dropService(Session session) {
		String service_name = session_to_servicename.get(session.getId());
		if(service_name == null) return;
		session_to_servicename.remove(session);
		List<Session> list = getServiceList(service_name);
		list.remove(session);
		logger.info("dropService("+service_name+",session:"+session.getId());
	}
	
	// 分发接收到的消息到实例
	public static void dispatchMessage(byte[] data) {
		if(data.length < 2) return;
		int json_length = (((int)data[0])<<8) + (int)data[1];
		JSONObject json = new JSONObject(new String(data,2,json_length));
		System.out.println(json);
		String task_id = json.getString("task_id");
		if(task_id == null) return;
		Callback callback = tasks.get(task_id);
		Session session = task_to_session.get(task_id);
		if(callback != null) {
			callback.recv(task_id,session,json,data,json_length); //再分发消息给具体的任务
		}
	}
	
	private static int task_id_count = 1000;
	
	static class TaskBundle {
		TaskBundle(Session session,String task_id) {
			this.session = session;
			this.task_id = task_id;
		}
		Session session;
		String task_id;
	}
	
	// 删除任务。
	// 意味着：该任务的结果已经在本服务器记录 或者 任务失败。
	public static void dropTask(String task_id) {
		tasks.remove(task_id);
		task_to_session.remove(task_id);
		task_notify_session.remove(task_id);
	}
	
	// 通知前端，告知任务状况
	public static void notifyTask(String task_id,String message) {
		Session session = task_notify_session.get(task_id);
		if(session == null) {
			return; //通知失败，因为前端还没有和该服务器建立WebSocket连接
		}
		try {
			session.getBasicRemote().sendText(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// 添加任务
	// @param service_name 该任务使用的服务名称
	// @param data 该任务的任务信息，为null则不发送
	// @param callback 该任务，服务端发来的消息
	// @return Session: 服务实例Session
	public static TaskBundle addTask(String service_name,Callback callback) {
		if(service_name == null) {
			return null;
		}
		//1.查找一个服务实例来完成该任务
		List<Session> session_list = services.get(service_name);
		if(session_list == null) {
			return null;
		}
		int n = session_list.size();
		if(0 == n) {
			return null; //没有该服务的实例
		}
		Session session = session_list.get((int)(Math.random()*n));
		
		if(session.isOpen() == false) {
			//Session已关闭
			dropService(session); //移除该服务
			return addTask(service_name,callback); //递归调用自身
		}
		
		//2.分配一个任务ID. 由于任务不需要持久化存储，所以每次重新计数即可。
		String task_id = ""+(task_id_count++);
		tasks.put(task_id,callback);
		task_to_session.put(task_id,session);
		
		return new TaskBundle(session,task_id);
	}
	
	//设置任务监听WebSocket
	public static boolean setTaskListener(String task_id,Session session) {
		if(tasks.contains(task_id) == false)
			return false; //不存在的任务
		task_notify_session.put(task_id,session);
		return true;
	}
}
