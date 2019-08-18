package cn.mldn.util.servlet;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import cn.mldn.util.BeanOperateUtil;
import cn.mldn.util.SplitUtil;
import cn.mldn.util.ValidatorUtil;

import com.jspsmart.upload.SmartUpload;
/**
 * 定义�?个全�?的公共的Servlet类，�?有的请求处理都将交给此类完成
 * @author mldn
 */
@SuppressWarnings("serial")
public abstract class DispatcherServlet extends HttpServlet {
	protected HttpServletRequest request ;
	protected HttpServletResponse response ;
	private ResourceBundle pagesResource ;
	private ResourceBundle messagesResource ;
	private ResourceBundle validatorResource ;
	private SmartUpload smart = null ;
	@Override
	public void init() throws ServletException {
		this.pagesResource = ResourceBundle.getBundle("Pages", Locale.getDefault()) ;
		this.messagesResource = ResourceBundle.getBundle("Messages", Locale.getDefault()) ;
		this.validatorResource = ResourceBundle.getBundle("Validator", Locale.getDefault()) ;
	} 
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String path = this.getPage("error.page") ;
		this.request = request ;
		this.response = response ;
		try {
			// 处理表单是否封装�?定要看它的MIME类型
			// 普�?�文本：application/x-www-form-urlencoded
			if (this.isUpload()) {	// 有上�?
				this.smart = new SmartUpload() ;
				this.smart.initialize(super.getServletConfig(), request,
						response);
				this.smart.upload();
			}
			String uri = request.getRequestURI();	// 接收分发参数
			String status = uri.substring(uri.lastIndexOf("/") + 1);
			boolean flag = true ;	// 数据验证
			try {
				// 对接收到的数据按照指定的验证规则进行验证�?
				// 确定要读取的key的名�?
				String rules [] = uri.split("/") ;
				String key = rules[rules.length-2] + "." + status + ".rule" ;
				// 进行指定规则的验证处�?
				ValidatorUtil vu = new ValidatorUtil(this.getValidator(key),this) ;
				flag = vu.getErrors().size() == 0 ;
				if (flag == false) {
					this.request.setAttribute("errors", vu.getErrors());
				} 
			} catch (Exception e) {}
			if (flag) {	// 没有错误，则继续向下执行
				// 将接收到的参数进行相应的设置
				Enumeration<String> enu = this.getParameterNames() ;
				while (enu.hasMoreElements()) {
					String name = enu.nextElement() ;	// 取得�?有参数名�?
					if (name.contains("[]")) {	// 表示是数�?
						String value [] = this.getParameterValues(name) ;
						BeanOperateUtil bou = new BeanOperateUtil(this,name,value) ;
					} else {
						String value = this.getParameter(name) ;
						BeanOperateUtil bou = new BeanOperateUtil(this,name,value) ;
					}
				}
				Method met = this.getClass().getMethod(status);
				path = this.getPage(met.invoke(this).toString());	// 执行具体的业务操�?
			} 
		} catch (Exception e) {
			e.printStackTrace(); 
		} 
		request.getRequestDispatcher(path).forward(request, response);
	} 
	
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		this.doGet(request, response);
	}
	/**
	 * 执行文件的删除处理操�?
	 * @param fileName
	 */
	public void deleteFile(String fileName) {
		File file = new File(super.getServletContext().getRealPath(this.getUploadDir()) + fileName) ;
		if (file.exists()) {
			file.delete() ;
		}
	}
	/**
	 * 执行多个文件的删除操�?
	 * @param files
	 */
	public void deleteFile(List<String> files) {
		Iterator<String> iter = files.iterator() ;
		while (iter.hasNext()) {
			this.deleteFile(iter.next()); 
		}
	}
	
	/**
	 * 取得HttpSession接口对象
	 * @return
	 */
	public HttpSession getSession() {
		return this.request.getSession() ;
	}
	/**
	 * 判断当前是否是表单的上传提交处理
	 * @return 如果为上传处理则返回true，否则返回false
	 */
	public boolean isUpload() {
		if (this.request.getContentType() != null) {
			if (this.request.getContentType().contains("multipart/form-data")) {
				return true ;
			}
		}
		return false ;
	}
	/**
	 * 得到�?有请求参数的名字，此处要考虑到表单封装的问题
	 * @return �?有名字的参数Enumeration接口
	 */
	@SuppressWarnings("unchecked")
	public Enumeration<String> getParameterNames() {
		if (this.isUpload()) {
			return this.smart.getRequest().getParameterNames() ;
		}
		return this.request.getParameterNames() ;
	} 
	/**
	 * 根据参数名称取得参数内容
	 * @param name 参数名称
	 * @return 参数内容
	 */
	public String getParameter(String name) {
		if (this.isUpload()) {
			return this.smart.getRequest().getParameter(name) ;
		}
		return this.request.getParameter(name) ;
	}
	/**
	 * 得到上传文件的名称，利用UUID生成
	 * @return 返回�?有的生成的上传文件名�?
	 */
	public List<String> createUploadFileName(){
		if (!this.isUpload()) {	// 没有文件上传
			return null ;
		}
		List<String> allNames = new ArrayList<String>() ;
		if (this.existsUpload()) {	// 整体有文件上�?
			for (int x = 0 ; x < this.smart.getFiles().getCount() ; x ++) {
				if (this.smart.getFiles().getFile(x).getSize() > 0) {
					String fileName = UUID.randomUUID() + "." + this.smart.getFiles().getFile(x).getFileExt() ;
					allNames.add(fileName) ;
				}
			}
		}
		return allNames ;
	} 
	/**
	 * 判断当前是否存在有上传文件的内容
	 * @return 如果存在上传文件则返回true，否则返回false
	 */
	public boolean existsUpload() {
		boolean flag = false ;
		if (!this.isUpload()) {
			return false ;
		}
		try {
			flag = this.smart.getFiles().getSize() > 0 ;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return flag ;
	}
	
	public boolean saveUploadFile(List<String> names) {
		boolean flag = true ;
		if (this.isUpload() && this.existsUpload()) {	// 表单封装并且上传�?
			for (int x = 0 ; x < this.smart.getFiles().getCount() ; x ++) {
				if (this.smart.getFiles().getFile(x).getSize() > 0) {
					String filePath = super.getServletContext().getRealPath(
							this.getUploadDir())
							+ names.get(x);
					File file = new File(filePath) ;
					if (!file.getParentFile().exists()) {
						file.mkdirs() ;	// 创建目录
					}
					try {
						this.smart.getFiles().getFile(x).saveAs(filePath);
					} catch (Exception e) {
						e.printStackTrace();
						flag = false ;
					}
				}
			}
		}
		return flag ;
	}
	/**
	 * 根据参数名称取得�?组参数内�?
	 * @param name 参数名称
	 * @return
	 */
	public String [] getParameterValues(String name) {
		if (this.isUpload()) {
			return this.smart.getRequest().getParameterValues(name) ;
		}
		return this.request.getParameterValues(name) ;
	} 
	/**
	 * 设置更新修改后的提示信息与路径，同时利用request范围传�??
	 * @param msgKey 消息的key
	 * @param urlKey 路径的key
	 */
	public void setMsgAndUrl(String msgKey,String urlKey) {
		this.request.setAttribute("msg", this.getMessage(msgKey));
		this.request.setAttribute("url", this.getPage(urlKey));
	}
	/**
	 * 取得指定key对应的路径信息，读取Pages.properties文件内容
	 * @param key 指定读取的key的内�?
	 * @return 将返回对应的key的数据，如果没有返回null
	 */
	public String getPage(String key) {
		return this.pagesResource.getString(key) ;
	}
	/**
	 * 获得指定的验证规则操�?
	 * @param key 验证规则的key
	 * @return �?组验证规则�?�参�?:类型|参数:类型�?
	 */
	private String getValidator(String key) {
		return this.validatorResource.getString(key) ;
	}
	/**
	 * 取得指定的消息的文字信息，将利用给定的内容（getMarkTitle()由子类覆写）动�?�设置占位符信息
	 * @param key 要读取的资源文件的key
	 * @return 格式化后的文本数�?
	 */
	public String getMessage(String key) {
		return MessageFormat.format(this.messagesResource.getString(key), this.getMarkTitle()) ;
	}
	/**
	 * 用户可以自己传�?�属于自己的参数占位符标�?
	 * @param key 要读取的资源文件的key
	 * @param args 占位符的内容
	 * @return 格式化后的文本数�?
	 */
	public String getMessage(String key,Object ... args) {
		return MessageFormat.format(this.messagesResource.getString(key), args) ;
	}
	/**
	 * 取得每一个具体的操作类型的名�?
	 * @return 返回名字，名字由子类决定
	 */
	public abstract String getMarkTitle() ;
	/**
	 * 表示要定义的上传目录
	 * @return
	 */
	public abstract String getUploadDir() ;
	/**
	 * 取得分页查询时默认的查询�?
	 * @return
	 */
	public abstract String getDefaultColumn() ;
	/**
	 * 取得默认分页查询时检索的字段名称，结构�?�显示标�?:列名称|显示标签|列名称�??
	 * @return
	 */
	public abstract String getColumntData() ; 
	/**
	 * 处理分页中的�?有参�?
	 */
	public SplitUtil handleSplitParam() {
		SplitUtil su = new SplitUtil() ;
		su.setCp(request.getParameter("cp"));
		String col = this.request.getParameter("col") ;
		if (col == null || "".equals(col)) {	// 现在没有col参数
			su.setCol(this.getDefaultColumn()); 	// 如果没有分页的列那么使用默认的列
		} else {
			su.setCol(col);
		}
		String kw = this.request.getParameter("kw") ;
		if (kw == null) {
			kw = "" ;
		}
		su.setKw(kw);
		this.request.setAttribute("currentPage", su.getCurrentPage());
		this.request.setAttribute("lineSize", su.getLineSize());
		this.request.setAttribute("keyWord", su.getKeyWord());
		this.request.setAttribute("column", su.getColumn());
		this.request.setAttribute("columnData",this.getColumntData());
		return su ;
	}
	
	
}
