

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import javax.websocket.Session;

import org.json.JSONObject;

/**
 * Servlet implementation class Servlet_PdfTableExtract
 */
@WebServlet("/table-extract")
@MultipartConfig //允许文件上传
public class Servlet_PdfTableExtract extends HttpServlet {
	private static final long serialVersionUID = 1L;
     
	// 记录任务结果
	class TaskResult {
		boolean isTaskFinished = false; //任务是否完成
		String zip_path; //任务结果(.zip)
	}
	
	private ConcurrentHashMap<String,TaskResult> task_results;
	
	// 服务消息处理器
	class MyDealer implements ServiceManager.Callback {
		@Override
		public void recv(String task_id, Session session, JSONObject json, byte[] raw_data,
					int data_pos) {
			boolean finished = json.getBoolean("finished"); //任务是否完成
			if(finished) {
				//已完成的话，raw_data从64字节往后即一个zip文件
				System.out.println("Task "+task_id+" finished.");
			}
		}
	}
	
    public Servlet_PdfTableExtract() {
        super();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("application/json; charset=UTF-8");
	    response.setCharacterEncoding("UTF-8");
	    String action = request.getParameter("action");
	    if(action == null) {
	    	return;
	    }
	    if(action.equals("addTask")) {
		    //添加表格提取任务
	    	//获取pdf文件
	    	Part part = null;
	    	try {
	    		part = request.getPart("file");
	    	} catch(Exception exception) {
	    		response.setStatus(400);
				response.getWriter().write("{\"errorMsg\":\"请正确上传PDF文件\"}");
	    	}
	    	System.out.println("part file.size:"+part.getSize());
	    	
	    	byte[] fileData;
            try (InputStream inputStream = part.getInputStream()) {
                fileData = inputStream.readAllBytes();
            }
		    ServiceManager.TaskBundle taskBundle = 
		    		ServiceManager.addTask("PDFTableExtract",new MyDealer());
		    if(taskBundle == null) {
		    	//添加任务失败
		    	response.setStatus(400);
				response.getWriter().write("{\"errorMsg\":\"名称为PDFTableExtract的服务无正在运行的实例。请联系开发人员\"}");
				return;
		    }
		    ByteBuffer data = Helper.mergeJsonAndData(
		    		new JSONObject().put("oneExcelFile",false)
		    					    .put("action","extract")
		    					    .put("task_id",taskBundle.task_id)
		    		,fileData
		    	);
	        try {
	        	taskBundle.session.getBasicRemote().sendBinary(data);
			} catch (IOException e) {
				e.printStackTrace();
			}
		    //返回task_id
		    response.getWriter().write(String.format("{\"task_id\":\"%s\"",taskBundle.task_id));
	    }  
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
