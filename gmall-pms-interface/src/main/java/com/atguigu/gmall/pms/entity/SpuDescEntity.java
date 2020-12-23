package com.atguigu.gmall.pms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * spu信息介绍
 * 
 * @author yige
 * @email 1020972669@qq.com
 * @date 2020-12-14 19:29:38
 */
@Data
@TableName("pms_spu_desc")
public class SpuDescEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 商品id
	 */
	//使用spu的主键id，因为在yml文件中设置id为auto，所以要把spuId设置为手动设置。
	@TableId(type = IdType.INPUT)
	private Long spuId;
	/**
	 * 商品介绍
	 */
	private String decript;

}
