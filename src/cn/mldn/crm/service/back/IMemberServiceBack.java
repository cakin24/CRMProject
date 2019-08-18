package cn.mldn.crm.service.back;

import java.util.Map;
import java.util.Set;
import cn.mldn.crm.vo.Member;

public interface IMemberServiceBack {
	// 实现用户登录操作
	public Map<String,Object> login(Member vo) throws Exception ;

	// 密码修改操作
	public boolean editPassword(String mid,String newPass,String oldPass) throws Exception ;

	// 增加数据前的数据查询处理
	public Map<String,Object> addPre(String mid) throws Exception ; 

	// 用户数据添加处理
	public boolean add(String mid, Member vo) throws Exception;

	// 数据的分页列表显示
	public Map<String,Object> list(String mid,String column,String keyWord,int currentPage,int lineSize) throws Exception ;

	// 修改前数据查询处理
	public Map<String,Object> editPre(String mid,String umid) throws Exception ;

	// 数据修改操作
	public boolean edit(String mid,Member vo) throws Exception ;

	// 删除指定用户信息
	public boolean editPasswordByAdmin(String mid,Member vo) throws Exception ;

	public boolean rm(String mid,Set<String> ids ) throws Exception ;
}
