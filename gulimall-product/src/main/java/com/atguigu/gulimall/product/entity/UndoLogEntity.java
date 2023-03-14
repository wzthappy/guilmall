//package com.atguigu.gulimall.product.entity;
//
//import com.baomidou.mybatisplus.annotation.TableId;
//import com.baomidou.mybatisplus.annotation.TableName;
//
//import java.io.Serializable;
//import java.util.Date;
//import lombok.Data;
//
///**
// *
// *
// * @author happy
// * @email sunlightcs@gmail.com
// * @date 2022-12-14 15:23:10
// */
//@Data
//@TableName("undo_log")
//public class UndoLogEntity implements Serializable {
//	private static final long serialVersionUID = 1L;
//
//	/**
//	 *
//	 */
//	@TableId
//	private Long id;
//	/**
//	 *
//	 */
//	private Long branchId;
//	/**
//	 *
//	 */
//	private String xid;
//	/**
//	 *
//	 */
//	private String context;
//	/**
//	 *
//	 */
//	private Longblob rollbackInfo;
//	/**
//	 *
//	 */
//	private Integer logStatus;
//	/**
//	 *
//	 */
//	private Date logCreated;
//	/**
//	 *
//	 */
//	private Date logModified;
//	/**
//	 *
//	 */
//	private String ext;
//
//}
