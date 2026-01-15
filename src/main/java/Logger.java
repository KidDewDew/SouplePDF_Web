
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 简单的日志工具类
 * 提供info、error、warning三个日志级别
 * 日志写入到当前目录下的log.txt文件
 */
public class Logger {
    
    private static Logger instance;
    private PrintWriter writer;
    private final ReentrantLock lock = new ReentrantLock();
    private static final String LOG_FILE = "log.txt";
    private static final SimpleDateFormat DATE_FORMAT = 
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    
    public static final String INFO = "INFO";
    public static final String ERROR = "ERROR";
    public static final String WARNING = "WARNING";
    
    private Logger() {
        initLogger();
    }
    
    /**
     * 获取Logger实例（单例）
     */
    public static Logger getInstance() {
        if (instance == null) {
            synchronized (Logger.class) {
                if (instance == null) {
                    instance = new Logger();
                }
            }
        }
        return instance;
    }
    
    /**
     * 初始化日志写入器
     */
    private void initLogger() {
        try {
            File logFile = new File(LOG_FILE);
            
            // 如果文件不存在则创建，如果存在则追加内容
            boolean isNewFile = !logFile.exists();
            writer = new PrintWriter(new FileWriter(logFile, true), true);
            
            // 如果是新文件，添加文件头
            if (isNewFile) {
                writer.println("=".repeat(50));
                writer.println("日志文件创建时间: " + DATE_FORMAT.format(new Date()));
                writer.println("=".repeat(50));
                writer.println();
            }
            
            // 添加关闭钩子，确保程序退出时关闭writer
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (writer != null) {
                    writer.close();
                }
            }));
            
        } catch (IOException e) {
            System.err.println("初始化日志文件失败: " + e.getMessage());
            // 失败时使用System.err作为后备
            writer = new PrintWriter(System.err, true);
        }
    }
    
    /**
     * 写入日志的通用方法
     * @param level 日志级别
     * @param message 日志信息
     * @param throwable 异常对象（可选）
     */
    private void log(String level, String message, Throwable throwable) {
        lock.lock();
        try {
            String timestamp = DATE_FORMAT.format(new Date());
            String threadName = Thread.currentThread().getName();
            
            // 构建日志行
            String logEntry = String.format("[%s] [%s] [%s] - %s", 
                    timestamp, level, threadName, message);
            
            // 写入文件
            writer.println(logEntry);
            
            // 如果有异常，打印异常堆栈
            if (throwable != null) {
                throwable.printStackTrace(writer);
            }
            
            // 同时在控制台输出（便于调试）
            System.out.println(logEntry);
            
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 记录INFO级别的日志
     * @param message 日志信息
     */
    public void info(String message) {
        log(INFO, message, null);
    }
    
    /**
     * 记录INFO级别的日志（带格式化）
     * @param format 格式化字符串
     * @param args 参数
     */
    public void info(String format, Object... args) {
        info(String.format(format, args));
    }
    
    /**
     * 记录WARNING级别的日志
     * @param message 日志信息
     */
    public void warning(String message) {
        log(WARNING, message, null);
    }
    
    /**
     * 记录WARNING级别的日志（带格式化）
     * @param format 格式化字符串
     * @param args 参数
     */
    public void warning(String format, Object... args) {
        warning(String.format(format, args));
    }
    
    /**
     * 记录ERROR级别的日志
     * @param message 日志信息
     */
    public void error(String message) {
        log(ERROR, message, null);
    }
    
    /**
     * 记录ERROR级别的日志（带异常）
     * @param message 日志信息
     * @param throwable 异常对象
     */
    public void error(String message, Throwable throwable) {
        log(ERROR, message, throwable);
    }
    
    /**
     * 记录ERROR级别的日志（带格式化和异常）
     * @param throwable 异常对象
     * @param format 格式化字符串
     * @param args 参数
     */
    public void error(Throwable throwable, String format, Object... args) {
        error(String.format(format, args), throwable);
    }
    
    /**
     * 关闭日志写入器
     */
    public void close() {
        lock.lock();
        try {
            if (writer != null) {
                writer.println();
                writer.println("日志结束时间: " + DATE_FORMAT.format(new Date()));
                writer.println();
                writer.close();
                writer = null;
            }
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 清理日志文件（清空内容）
     */
    public void clearLog() {
        lock.lock();
        try {
            close();
            writer = new PrintWriter(new FileWriter(LOG_FILE, false), true);
            writer.println("日志文件清空时间: " + DATE_FORMAT.format(new Date()));
            writer.println();
        } catch (IOException e) {
            System.err.println("清空日志文件失败: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }
}
