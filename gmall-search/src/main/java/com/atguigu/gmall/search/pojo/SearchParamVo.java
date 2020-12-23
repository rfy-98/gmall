package com.atguigu.gmall.search.pojo;

import lombok.Data;

import java.util.List;

@Data
public class SearchParamVo {
    //search?keyword=小米&brandId=1,3&cid=225&props=5:高通-麒麟&props=6:骁龙865-硅谷1000&sort=1&priceFrom=1000&priceTo=6000&pageNum=1&store=true
    //搜索关键字
    private String keyword;
    //搜索品牌过滤
    private List<Long> brandId;
    //搜索分类过滤
    private Long cid ;
    //搜索检索参数
    private List<String> props;
    //排序  排序字段：0-默认，得分降序；1-按价格升序；2-按价格降序；3-按创建时间降序；4-按销量降序
    private Integer sort = 0;
    //价格区间
    private Double priceFrom;
    private Double priceTo;
    //分页
    private Integer pageNum = 1;
    private Integer pageSize = 20;
    //库存是否有货
    private Boolean store;

}
